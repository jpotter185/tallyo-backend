package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class StandingsTeamResponse {
    private String id;
    private String name;
    private String abbreviation;
    private String logo;
    private String location;
    private String seed;
    private String conference;
    private String division;
    private String record;
    private String differential;

    /**
     * Dynamic ESPN stat map: stat type -> displayValue
     * (points, gamesplayed, winpercent, pointsfor, pointsagainst, streak, ...).
     * Keys vary by league.
     */
    private Map<String, String> stats;
}
