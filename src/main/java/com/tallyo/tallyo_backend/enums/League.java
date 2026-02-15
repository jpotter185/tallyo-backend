package com.tallyo.tallyo_backend.enums;

import lombok.Getter;

@Getter
public enum League {
    NFL(Sport.FOOTBALL, "nfl", true, true),
    CFB(Sport.FOOTBALL, "college-football", true, true),
    NHL(Sport.HOCKEY, "nhl", false, false);
    private final String value;
    private final Sport sport;
    private final boolean supportsYearFilter;
    private final boolean supportsWeekFilter;

    private League(Sport sport, String value, boolean supportsYearFilter, boolean supportsWeekFilter) {
        this.value = value;
        this.sport = sport;
        this.supportsYearFilter = supportsYearFilter;
        this.supportsWeekFilter = supportsWeekFilter;
    }
}
