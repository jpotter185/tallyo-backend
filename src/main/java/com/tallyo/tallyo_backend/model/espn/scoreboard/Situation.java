package com.tallyo.tallyo_backend.model.espn.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Situation {
    private String possession;
    private String downDistanceText;
    private String shortDownDistanceText;
    private String possessionText;
    private Integer homeTimeouts;
    private Integer awayTimeouts;
    private LastPlay lastPlay;
}
