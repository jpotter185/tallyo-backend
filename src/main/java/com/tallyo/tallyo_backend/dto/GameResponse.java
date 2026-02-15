package com.tallyo.tallyo_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GameResponse {
    private String id;
    private String league;
    private TeamResponse homeTeam;
    private TeamResponse awayTeam;
    private String stadiumName;
    private String location;
    private String isoDate;
    private String date;
    private String homeScore;
    private String awayScore;
    private String period;
    private String shortPeriod;
    private String channel;
    private String espnLink;
    private String lastPlay;
    private String currentDownAndDistance;
    private String down;
    private String ballLocation;
    private String possessionTeamId;
    private Integer homeTimeouts;
    private Integer awayTimeouts;
    private String winner;
    private String headline;
    private GameOddResponse gameOdd;
    private String gameStatus;
    private StatResponse stats;
    private String homeWinPercentage;
    private String awayWinPercentage;
    private Boolean finalGame;
    @JsonProperty("final")
    private Boolean finalValue;
    private String homeRecordAtTimeOfGame;
    private String awayRecordAtTimeOfGame;
}
