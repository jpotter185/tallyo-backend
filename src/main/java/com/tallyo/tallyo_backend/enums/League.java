package com.tallyo.tallyo_backend.enums;

import lombok.Getter;

@Getter
public enum League {
    NFL(Sport.FOOTBALL, "nfl", "NFL", true, true, true, "season", "football", "away-left", true, true),
    CFB(Sport.FOOTBALL, "college-football", "CFB", true, true, true, "season", "football", "away-left", true, true),
    NHL(Sport.HOCKEY, "nhl", "NHL", false, false, false, "date", "hockey", "away-left", false, false),
    MLS(Sport.SOCCER, "usa.1", "MLS", false, false, false, "date", "soccer", "home-left", false, false);

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
           boolean supportsLiveDetails) {
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
    }

    public String getId() {
        return name().toLowerCase();
    }

    public String getPath() {
        return "/" + getId();
    }
}
