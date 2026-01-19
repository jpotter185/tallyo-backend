package com.tallyo.tallyo_backend.model.espn.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Team {
    private int id;
    private String uid;
    private String location;
    private String name;
    private String abbreviation;
    private String displayName;
    private String shortDisplayName;
    private String color;
    private String alternateColor;
    private boolean isActive;
    private TeamVenue venue;
    private String logo;

    // getters & setters
}

