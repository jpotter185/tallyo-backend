package com.tallyo.tallyo_backend.enums;

import lombok.Getter;

@Getter
public enum League {
    NFL("nfl"),
    CFB("college-football");
    private final String value;
    private League(String value){
        this.value = value;
    }

}
