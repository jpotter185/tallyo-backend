package com.tallyo.tallyo_backend.model.espn.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Season {
    private int type;
    private int year;

    // getters & setters
}
