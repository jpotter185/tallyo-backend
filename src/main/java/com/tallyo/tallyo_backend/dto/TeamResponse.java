package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TeamResponse {
    private TeamKeyResponse teamKey;
    private String id;
    private String name;
    private String abbreviation;
    private String logo;
    private String primaryColor;
    private String alternateColor;
    private String location;
    private String record;
    private String seed;
    private String ranking;
    private String wins;
    private String losses;
    private String ties;
    private String conference;
    private String division;
    private String homeRecord;
    private String roadRecord;
    private String recordVsConference;
    private String recordVsDivision;
    private String pointsFor;
    private String pointsAgainst;
    private String pointDifferential;
    private String streak;
    private String winPercent;
    private String score;
}
