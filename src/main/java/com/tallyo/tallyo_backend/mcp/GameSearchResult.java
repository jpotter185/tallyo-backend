package com.tallyo.tallyo_backend.mcp;

import java.util.List;

/**
 * @param matched   games matching the filters
 * @param returned  games in this response (at most the requested limit)
 * @param truncated true when more games matched than were returned; narrow the filters
 */
public record GameSearchResult(int matched, int returned, boolean truncated, List<GameSummary> games) {
}
