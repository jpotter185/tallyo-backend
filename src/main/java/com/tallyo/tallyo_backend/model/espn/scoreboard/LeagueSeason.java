package com.tallyo.tallyo_backend.model.espn.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class LeagueSeason {
    private int year;
    private String startDate;
    private String endDate;
    private String displayName;
    private SeasonType type;
}
