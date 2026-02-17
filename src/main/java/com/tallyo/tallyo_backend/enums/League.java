package com.tallyo.tallyo_backend.enums;

import lombok.Getter;

@Getter
public enum League {
    NFL(Sport.FOOTBALL, "nfl", "NFL", true, true, true, "season", "football"),
    CFB(Sport.FOOTBALL, "college-football", "CFB", true, true, true, "season", "football"),
    NHL(Sport.HOCKEY, "nhl", "NHL", false, false, false, "date", "hockey");

    private final String value;
    private final String label;
    private final Sport sport;
    private final boolean supportsYearFilter;
    private final boolean supportsWeekFilter;
    private final boolean supportsStandings;
    private final String contextMode;
    private final String statsProfile;

    League(Sport sport,
           String value,
           String label,
           boolean supportsYearFilter,
           boolean supportsWeekFilter,
           boolean supportsStandings,
           String contextMode,
           String statsProfile) {
        this.value = value;
        this.label = label;
        this.sport = sport;
        this.supportsYearFilter = supportsYearFilter;
        this.supportsWeekFilter = supportsWeekFilter;
        this.supportsStandings = supportsStandings;
        this.contextMode = contextMode;
        this.statsProfile = statsProfile;
    }

    public String getId() {
        return name().toLowerCase();
    }

    public String getPath() {
        return "/" + getId();
    }
}
