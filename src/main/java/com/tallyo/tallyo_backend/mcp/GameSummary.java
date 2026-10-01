package com.tallyo.tallyo_backend.mcp;

import com.tallyo.tallyo_backend.dto.GameResponse;
import com.tallyo.tallyo_backend.dto.TeamResponse;

/**
 * A game trimmed to what's useful in a chat: the full {@link GameResponse} carries logos,
 * colors and live play-by-play fields that would flood a model's context.
 *
 * @param id     ESPN game id; pass to get_game_details
 * @param status ESPN status, e.g. STATUS_SCHEDULED, STATUS_IN_PROGRESS, STATUS_FINAL
 * @param clock  short period/clock text, e.g. "Final", "Q3 4:12"
 * @param spread betting line as captured, e.g. "BUF -6.5" (absent once ESPN drops it)
 * @param winner winning team's abbreviation when final
 */
public record GameSummary(
        String id,
        String league,
        String start,
        String status,
        String clock,
        String venue,
        Team away,
        Team home,
        String winner,
        String spread
) {
    /** @param record the team's record going into this game */
    public record Team(String abbreviation, String name, String score, String record) {
    }

    static GameSummary from(GameResponse g) {
        return new GameSummary(
                g.getId(),
                g.getLeague(),
                g.getIsoDate(),
                g.getGameStatus(),
                g.getShortPeriod(),
                g.getStadiumName(),
                team(g.getAwayTeam(), g.getAwayScore(), g.getAwayRecordAtTimeOfGame()),
                team(g.getHomeTeam(), g.getHomeScore(), g.getHomeRecordAtTimeOfGame()),
                winnerAbbreviation(g),
                g.getGameOdd() == null ? null : g.getGameOdd().getSpreadText()
        );
    }

    private static Team team(TeamResponse t, String score, String record) {
        return t == null ? null : new Team(t.getAbbreviation(), t.getName(), score, record);
    }

    /** GameResponse.winner holds the winning team's id; report its abbreviation instead. */
    private static String winnerAbbreviation(GameResponse g) {
        String winner = g.getWinner();
        if (winner == null || winner.isBlank()) {
            return null;
        }
        for (TeamResponse t : new TeamResponse[]{g.getHomeTeam(), g.getAwayTeam()}) {
            if (t != null && (winner.equals(t.getId()) || winner.equalsIgnoreCase(t.getAbbreviation()))) {
                return t.getAbbreviation();
            }
        }
        return winner;
    }
}
