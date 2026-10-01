package com.tallyo.tallyo_backend.mcp;

import com.tallyo.tallyo_backend.dto.GameOddResponse;
import com.tallyo.tallyo_backend.dto.GameResponse;
import com.tallyo.tallyo_backend.dto.InjuryReportResponse;
import com.tallyo.tallyo_backend.dto.TeamInjuryReportResponse;
import com.tallyo.tallyo_backend.dto.TeamResponse;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.exception.InvalidRequestException;
import com.tallyo.tallyo_backend.mapper.GameResponseMapper;
import com.tallyo.tallyo_backend.service.CalendarService;
import com.tallyo.tallyo_backend.service.GameService;
import com.tallyo.tallyo_backend.service.InjuryService;
import com.tallyo.tallyo_backend.service.StandingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TallyoMcpToolsTest {

    private final GameService gameService = mock(GameService.class);
    private final InjuryService injuryService = mock(InjuryService.class);
    private final GameResponseMapper mapper = mock(GameResponseMapper.class);
    private final Map<Game, GameResponse> responses = new HashMap<>();
    private TallyoMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new TallyoMcpTools(gameService, mock(CalendarService.class), mock(StandingsService.class),
                injuryService, mapper);
        when(mapper.toResponse(any())).thenAnswer(inv -> responses.get(inv.getArgument(0)));
    }

    @Test
    void findGamesReturnsKickoffOrderEvenThoughTheQueryGroupsByStatus() {
        // The shared query puts upcoming games before finished ones; the tool must re-sort.
        stored(30, game("3", "2026-10-04T17:00:00Z", "NE", "BUF", "STATUS_SCHEDULED", null),
                game("1", "2026-09-13T17:00:00Z", "BUF", "HOU", "STATUS_FINAL", "BUF"),
                game("2", "2026-09-18T00:15:00Z", "DET", "BUF", "STATUS_FINAL", "BUF"));

        GameSearchResult result = tools.findGames("nfl", 2026, 2, null, null, null, null, null);

        assertThat(result.games()).extracting(GameSummary::id).containsExactly("1", "2", "3");
        assertThat(result.matched()).isEqualTo(30);
        assertThat(result.truncated()).isTrue();
    }

    @Test
    void limitCapsResultsAndIsClamped() {
        stored(3, game("1", "2026-09-10T00:20:00Z", "DAL", "PHI", "STATUS_FINAL", "PHI"),
                game("2", "2026-09-14T17:00:00Z", "BUF", "HOU", "STATUS_FINAL", "BUF"),
                game("3", "2026-09-21T17:00:00Z", "KC", "MIA", "STATUS_FINAL", "KC"));

        assertThat(tools.findGames("nfl", 2026, 2, null, null, null, 2, null).games())
                .extracting(GameSummary::id).containsExactly("1", "2");
        assertThat(tools.findGames("nfl", 2026, 2, null, null, null, 0, null).returned()).isEqualTo(1);
        assertThat(tools.findGames("nfl", 2026, 2, null, null, null, 10_000, null).returned()).isEqualTo(3);
    }

    @Test
    void teamFilterMatchesAbbreviationOrNameAndCountsOnlyThoseGames() {
        stored(3, game("1", "2026-09-13T17:00:00Z", "BUF", "HOU", "STATUS_FINAL", "BUF"),
                game("2", "2026-09-14T17:00:00Z", "KC", "MIA", "STATUS_FINAL", "KC"),
                game("3", "2026-09-18T00:15:00Z", "DET", "BUF", "STATUS_FINAL", "BUF"));

        GameSearchResult byAbbreviation = tools.findGames("nfl", 2026, 2, null, null, "buf", null, null);
        GameSearchResult byName = tools.findGames("nfl", 2026, 2, null, null, "Bills", null, null);

        assertThat(byAbbreviation.games()).extracting(GameSummary::id).containsExactly("1", "3");
        assertThat(byAbbreviation.matched()).isEqualTo(2);
        assertThat(byAbbreviation.truncated()).isFalse();
        assertThat(byName.games()).extracting(GameSummary::id).containsExactly("1", "3");
    }

    @Test
    void summaryReportsWinnerAndSpreadByAbbreviation() {
        stored(1, game("1", "2026-09-13T17:00:00Z", "BUF", "HOU", "STATUS_FINAL", "BUF"));

        GameSummary g = tools.findGames("nfl", 2026, 2, null, null, null, null, null).games().get(0);

        assertThat(g.winner()).isEqualTo("BUF");
        assertThat(g.spread()).isEqualTo("HOU -1.5");
        assertThat(g.away().abbreviation()).isEqualTo("BUF");
        assertThat(g.home().name()).isEqualTo("HOU Team");
    }

    @Test
    void leagueCodesAreCaseInsensitiveAndUnknownOnesExplainTheChoices() {
        assertThat(TallyoMcpTools.league("NFL")).isEqualTo(League.NFL);
        assertThat(TallyoMcpTools.league(" mlb ")).isEqualTo(League.MLB);
        assertThatThrownBy(() -> TallyoMcpTools.league("xfl"))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("nfl, cfb, nhl, mls, or mlb");
    }

    @Test
    void gameDetailsRejectsNonNumericIds() {
        assertThatThrownBy(() -> tools.getGameDetails("abc"))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("numeric");
    }

    @Test
    void injuriesFilterByTeamAndExplainMissingData() {
        TeamInjuryReportResponse kc = TeamInjuryReportResponse.builder().abbreviation("KC").build();
        TeamInjuryReportResponse buf = TeamInjuryReportResponse.builder().abbreviation("BUF").build();
        when(injuryService.getInjuries(League.NFL))
                .thenReturn(new InjuryReportResponse("nfl", "2026-09-30T12:00:00Z", List.of(kc, buf)));

        assertThat(tools.getInjuries("nfl", "kc").getTeams()).containsExactly(kc);
        assertThat(tools.getInjuries("nfl", null).getTeams()).hasSize(2);
        assertThatThrownBy(() -> tools.getInjuries("nfl", "XYZ"))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("[BUF, KC]");

        when(injuryService.getInjuries(League.NFL)).thenReturn(new InjuryReportResponse("nfl", null, List.of()));
        assertThatThrownBy(() -> tools.getInjuries("nfl", "KC"))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("No injury data");
    }

    // ---- helpers ----

    /**
     * The service returns these games (in the given order) out of {@code total} matches. A full
     * page (page size = games given) lets {@code total} exceed it, standing in for more matches
     * than the tool's scan limit.
     */
    private void stored(long total, Game... games) {
        when(gameService.getGames(eq(League.NFL), anyInt(), anyInt(), anyInt(), anyString(), anyString(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(games), PageRequest.of(0, games.length), total));
    }

    private Game game(String id, String start, String away, String home, String status, String winner) {
        Game game = mock(Game.class);
        responses.put(game, GameResponse.builder()
                .id(id).league("nfl").isoDate(start).gameStatus(status).shortPeriod("Final")
                .awayTeam(team(away)).homeTeam(team(home)).awayScore("24").homeScore("17")
                .winner(winner == null ? null : "id-" + winner)
                .gameOdd(GameOddResponse.builder().spreadText(home + " -1.5").build())
                .build());
        return game;
    }

    private static TeamResponse team(String abbreviation) {
        return TeamResponse.builder().id("id-" + abbreviation).abbreviation(abbreviation)
                .name(abbreviation.equals("BUF") ? "Buffalo Bills" : abbreviation + " Team").build();
    }
}
