package com.tallyo.tallyo_backend.mcp;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.dto.GameDetailsResponse;
import com.tallyo.tallyo_backend.dto.InjuryReportResponse;
import com.tallyo.tallyo_backend.dto.LeagueMetadataResponse;
import com.tallyo.tallyo_backend.dto.StandingsGroupResponse;
import com.tallyo.tallyo_backend.dto.TeamInjuryReportResponse;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.exception.InvalidRequestException;
import com.tallyo.tallyo_backend.mapper.GameResponseMapper;
import com.tallyo.tallyo_backend.service.CalendarService;
import com.tallyo.tallyo_backend.service.GameService;
import com.tallyo.tallyo_backend.service.InjuryService;
import com.tallyo.tallyo_backend.service.StandingsService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Read-only MCP tools over tallyo's stored ESPN data, served at /mcp (see SecurityConfig for
 * auth). Tools call the same services as the REST controllers; nothing here writes data or
 * triggers an ESPN fetch (the admin backfill is deliberately not exposed).
 */
@Component
public class TallyoMcpTools {

    static final String DEFAULT_TIME_ZONE = "America/New_York";
    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 300;
    /** Upper bound on games read per search (more than any league-season); see find_games. */
    static final int SCAN_LIMIT = 3000;

    private static final String LEAGUE_DESCRIPTION =
            "League code: nfl, cfb (college football), nhl, mls, or mlb";
    private static final String TIME_ZONE_DESCRIPTION =
            "IANA time zone for day boundaries and 'today', e.g. America/Chicago (default America/New_York)";

    private final GameService gameService;
    private final CalendarService calendarService;
    private final StandingsService standingsService;
    private final InjuryService injuryService;
    private final GameResponseMapper gameResponseMapper;

    public TallyoMcpTools(GameService gameService, CalendarService calendarService,
                          StandingsService standingsService, InjuryService injuryService,
                          GameResponseMapper gameResponseMapper) {
        this.gameService = gameService;
        this.calendarService = calendarService;
        this.standingsService = standingsService;
        this.injuryService = injuryService;
        this.gameResponseMapper = gameResponseMapper;
    }

    @McpTool(name = "list_leagues",
            description = "Leagues tallyo covers and what each supports (week/year filters vs. date-based "
                    + "schedules, standings, odds, player stats).",
            annotations = @McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false))
    public List<LeagueMetadataResponse> listLeagues() {
        return Arrays.stream(League.values()).map(LeagueMetadataResponse::fromLeague).toList();
    }

    @McpTool(name = "get_context",
            description = "The league's current point in its season: year, seasonType (1 preseason, "
                    + "2 regular season, 3 postseason), week (season leagues: nfl, cfb) or date (daily "
                    + "leagues: nhl, mls, mlb). Use it to fill in find_games filters for 'this week'.",
            annotations = @McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false))
    public CurrentContext getContext(
            @McpToolParam(description = LEAGUE_DESCRIPTION) String league,
            @McpToolParam(description = TIME_ZONE_DESCRIPTION, required = false) String timeZone) {
        return calendarService.getCurrentContext(league(league), timeZone(timeZone));
    }

    @McpTool(name = "get_current_games",
            description = "This week's games (nfl, cfb) or today's games (nhl, mls, mlb) with scores, "
                    + "status, records and spreads.",
            annotations = @McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false))
    public GameSearchResult getCurrentGames(
            @McpToolParam(description = LEAGUE_DESCRIPTION) String league,
            @McpToolParam(description = TIME_ZONE_DESCRIPTION, required = false) String timeZone) {
        League l = league(league);
        String tz = timeZone(timeZone);
        CurrentContext context = calendarService.getCurrentContext(l, tz);
        List<GameSummary> games = gameService
                .getCurrentGames(l, context, tz, PageRequest.of(0, MAX_LIMIT))
                .map(this::summary).stream()
                .sorted(Comparator.comparing(GameSummary::start, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return new GameSearchResult(games.size(), games.size(), false, games);
    }

    @McpTool(name = "find_games",
            description = "Search stored games. Filters combine; leave any out to not filter on it. "
                    + "Season leagues (nfl, cfb) use year + seasonType + week; daily leagues (nhl, mls, mlb) "
                    + "use date. Games come in kickoff order, oldest first. 'team' matches an abbreviation "
                    + "(BUF) or part of a name (Bills). Results are capped at 'limit'; 'truncated' says when "
                    + "more matched, so narrow by year, week or date (e.g. a team's season fits easily).",
            annotations = @McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false))
    public GameSearchResult findGames(
            @McpToolParam(description = LEAGUE_DESCRIPTION) String league,
            @McpToolParam(description = "Season year, e.g. 2026 (an NFL season is named for the year it starts)",
                    required = false) Integer year,
            @McpToolParam(description = "1 preseason, 2 regular season, 3 postseason", required = false)
            Integer seasonType,
            @McpToolParam(description = "Week number within the season type (nfl, cfb)", required = false)
            Integer week,
            @McpToolParam(description = "Day, YYYY-MM-DD, in the given time zone", required = false) String date,
            @McpToolParam(description = "Team abbreviation or part of its name", required = false) String team,
            @McpToolParam(description = "Max games to return (default 50, max 300)", required = false)
            Integer limit,
            @McpToolParam(description = TIME_ZONE_DESCRIPTION, required = false) String timeZone) {
        League l = league(league);
        int cap = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        // The shared query orders live, then upcoming, then finished games (for the UI), so read
        // the matches and put them in kickoff order here.
        var page = gameService.getGames(l, orZero(year), orZero(seasonType), orZero(week),
                date == null ? "" : date.trim(), timeZone(timeZone), PageRequest.of(0, SCAN_LIMIT));
        List<GameSummary> games = page.map(this::summary).stream()
                .filter(g -> team == null || team.isBlank() || involves(g, team.trim()))
                .sorted(Comparator.comparing(GameSummary::start, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        long matched = team == null || team.isBlank() ? page.getTotalElements() : games.size();
        List<GameSummary> returned = games.stream().limit(cap).toList();
        return new GameSearchResult((int) matched, returned.size(), matched > returned.size(), returned);
    }

    @McpTool(name = "get_game_details",
            description = "Box score for one game: stat leaders, scoring plays, and per-player stat groups. "
                    + "Use the game id from find_games or get_current_games.",
            annotations = @McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false))
    public GameDetailsResponse getGameDetails(@McpToolParam(description = "ESPN game id") String gameId) {
        try {
            return gameService.getGameDetails(Integer.parseInt(gameId.trim()));
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("gameId must be a numeric ESPN game id");
        }
    }

    @McpTool(name = "get_standings",
            description = "Current standings by division/conference group.",
            annotations = @McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false))
    public List<StandingsGroupResponse> getStandings(@McpToolParam(description = LEAGUE_DESCRIPTION) String league) {
        return standingsService.getStandings(league(league));
    }

    @McpTool(name = "get_injuries",
            description = "NFL injury report: each player's designation (Out, Questionable, ...) and the "
                    + "QB depth chart, refreshed every 6 hours. Pass 'team' to get one team; the full "
                    + "report is large.",
            annotations = @McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false))
    public InjuryReportResponse getInjuries(
            @McpToolParam(description = "League code (only nfl is supported)") String league,
            @McpToolParam(description = "Team abbreviation, e.g. KC", required = false) String team) {
        InjuryReportResponse report = injuryService.getInjuries(league(league));
        if (report.getTeams() == null || report.getTeams().isEmpty()) {
            throw new InvalidRequestException("No injury data has been loaded yet; it refreshes every 6 hours");
        }
        if (team == null || team.isBlank()) {
            return report;
        }
        List<TeamInjuryReportResponse> teams = report.getTeams().stream()
                .filter(t -> team.trim().equalsIgnoreCase(t.getAbbreviation()))
                .toList();
        if (teams.isEmpty()) {
            throw new InvalidRequestException("No injury report for team '" + team + "'. Teams: "
                    + report.getTeams().stream().map(TeamInjuryReportResponse::getAbbreviation).sorted().toList());
        }
        return new InjuryReportResponse(report.getLeague(), report.getUpdatedAt(), teams);
    }

    @McpTool(name = "get_game_dates",
            description = "Days with games for a daily league (nhl, mls, mlb), YYYY-MM-DD, to pick a date for find_games.",
            annotations = @McpAnnotations(readOnlyHint = true, idempotentHint = true, openWorldHint = false))
    public List<String> getGameDates(
            @McpToolParam(description = LEAGUE_DESCRIPTION) String league,
            @McpToolParam(description = TIME_ZONE_DESCRIPTION, required = false) String timeZone) {
        return calendarService.getGameDates(league(league), timeZone(timeZone));
    }

    // ---- helpers ----

    static League league(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidRequestException("league is required: nfl, cfb, nhl, mls, or mlb");
        }
        try {
            return League.valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown league '" + code + "': use nfl, cfb, nhl, mls, or mlb");
        }
    }

    private static String timeZone(String tz) {
        return tz == null || tz.isBlank() ? DEFAULT_TIME_ZONE : tz.trim();
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    static boolean involves(GameSummary game, String team) {
        return matches(game.home(), team) || matches(game.away(), team);
    }

    private static boolean matches(GameSummary.Team t, String team) {
        return t != null && (team.equalsIgnoreCase(t.abbreviation())
                || (t.name() != null && t.name().toLowerCase(Locale.ROOT).contains(team.toLowerCase(Locale.ROOT))));
    }

    private GameSummary summary(Game game) {
        return GameSummary.from(gameResponseMapper.toResponse(game));
    }
}
