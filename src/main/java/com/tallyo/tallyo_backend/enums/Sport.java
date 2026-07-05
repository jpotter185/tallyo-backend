package com.tallyo.tallyo_backend.enums;

import lombok.Getter;

@Getter
public enum Sport {
    HOCKEY("hockey"),
    FOOTBALL("football"),
    SOCCER("soccer"),
    BASEBALL("baseball");

    private final String value;

    Sport(String value) {
        this.value = value;
    }
}
