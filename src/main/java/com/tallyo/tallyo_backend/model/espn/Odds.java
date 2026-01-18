package com.tallyo.tallyo_backend.model.espn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Odds {
    Provider provider;
    String details;
    Integer overUnder;
    long spread;
    TeamOdds awayTeamOdds;
    TeamOdds homeTeamOdds;
    Moneyline moneyline;
    PointSpread pointSpread;
}

