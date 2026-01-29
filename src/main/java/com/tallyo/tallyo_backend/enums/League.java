package com.tallyo.tallyo_backend.enums;

import lombok.Getter;

@Getter
public enum League {
    NFL(Sport.FOOTBALL, "nfl"),
    CFB(Sport.FOOTBALL, "college-football"),
    NHL(Sport.HOCKEY, "nhl");
    private final String value;
    private final Sport sport;

    private League(Sport sport, String value) {
        this.value = value;
        this.sport = sport;
    }
}
