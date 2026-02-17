package com.tallyo.tallyo_backend.model.espn.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Odds {
    Provider provider;
    String details;
    Double overUnder;
    Double spread;
    TeamOdds awayTeamOdds;
    TeamOdds homeTeamOdds;
    Moneyline moneyline;
    PointSpread pointSpread;
}

