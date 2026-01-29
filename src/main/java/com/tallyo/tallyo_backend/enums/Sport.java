package com.tallyo.tallyo_backend.enums;

import lombok.Getter;

@Getter
public enum Sport {
    HOCKEY("hockey"),
    FOOTBALL("football");
    private final String value;

    private Sport(String value) {
        this.value = value;
    }
}
