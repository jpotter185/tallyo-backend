package com.tallyo.tallyo_backend.model.espn.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class League {
    private String id;
    private String uid;
    private String name;
    private String abbreviation;
    private String slug;
    private LeagueSeason season;
}

