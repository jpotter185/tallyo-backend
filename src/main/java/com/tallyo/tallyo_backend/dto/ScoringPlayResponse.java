package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ScoringPlayResponse {
    private String id;
    private String teamId;
    private String teamName;
    private String displayText;
    private Integer homeScore;
    private Integer awayScore;
    private String scoringType;
    private Integer period;
    private String clock;
}
