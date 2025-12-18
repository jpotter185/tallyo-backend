package com.tallyo.tallyo_backend.entity;

import com.tallyo.tallyo_backend.enums.League;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "games")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Game {
    @Id
    private int id;
    @Enumerated(EnumType.STRING)
    private League league;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;
    private String stadiumName;
    private String location;
    private String isoDate;
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
    private String gameStatus;
    private String homeWinPercentage;
    private String awayWinPercentage;
    private Boolean finalGame;
    private String week;

    public Game(int id, League league, Team homeTeam, Team awayTeam) {
        this.id = id;
        this.league = league;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }
}
