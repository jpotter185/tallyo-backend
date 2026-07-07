package com.tallyo.tallyo_backend.enums;

import lombok.Getter;

@Getter
public enum League {
    // Declaration order drives /api/v1/leagues and therefore the frontend tab order.
    WORLD_CUP(Sport.SOCCER, "fifa.world", "WC", false, false, true, "date", "soccer", "home-left", false, true, false),
    MLS(Sport.SOCCER, "usa.1", "MLS", false, false, true, "date", "soccer", "home-left", false, true, false),
    NFL(Sport.FOOTBALL, "nfl", "NFL", true, true, true, "season", "football", "away-left", true, true, false),
    CFB(Sport.FOOTBALL, "college-football", "CFB", true, true, true, "season", "football", "away-left", true, true, false),
    NHL(Sport.HOCKEY, "nhl", "NHL", false, false, true, "date", "hockey", "away-left", false, true, false),
    MLB(Sport.BASEBALL, "mlb", "MLB", false, false, true, "date", "baseball", "away-left", true, true, true);

    private final String value;
    private final String label;
    private final Sport sport;
    private final boolean supportsYearFilter;
    private final boolean supportsWeekFilter;
    private final boolean supportsStandings;
    private final String contextMode;
    private final String statsProfile;
    private final String teamOrder;
    private final boolean supportsOdds;
    private final boolean supportsLiveDetails;
    private final boolean supportsPlayerStats;

    League(Sport sport,
           String value,
           String label,
           boolean supportsYearFilter,
           boolean supportsWeekFilter,
           boolean supportsStandings,
           String contextMode,
           String statsProfile,
           String teamOrder,
           boolean supportsOdds,
           boolean supportsLiveDetails,
           boolean supportsPlayerStats) {
        this.value = value;
        this.label = label;
        this.sport = sport;
        this.supportsYearFilter = supportsYearFilter;
        this.supportsWeekFilter = supportsWeekFilter;
        this.supportsStandings = supportsStandings;
        this.contextMode = contextMode;
        this.statsProfile = statsProfile;
        this.teamOrder = teamOrder;
        this.supportsOdds = supportsOdds;
        this.supportsLiveDetails = supportsLiveDetails;
        this.supportsPlayerStats = supportsPlayerStats;
    }

    public String getId() {
        return name().toLowerCase();
    }

    public String getPath() {
        return "/" + getId();
    }
}
