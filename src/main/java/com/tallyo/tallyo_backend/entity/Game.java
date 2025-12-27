package com.tallyo.tallyo_backend.entity;

import com.tallyo.tallyo_backend.enums.League;
import jakarta.persistence.*;
import lombok.*;

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
    @JoinColumns({
            @JoinColumn(name = "home_team_id", referencedColumnName = "teamId"),
            @JoinColumn(name = "home_team_league", referencedColumnName = "league")
    })
    private Team homeTeam;
    private String homeRecordAtTimeOfGame;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "away_team_id", referencedColumnName = "teamId"),
            @JoinColumn(name = "away_team_league", referencedColumnName = "league")
    })
    private Team awayTeam;
    private String awayRecordAtTimeOfGame;
    private int week;
    private int seasonType;
    private int year;
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
    private int winner;
    private String headline;
    private String gameStatus;
    private String homeWinPercentage;
    private String awayWinPercentage;
    private Boolean finalGame;

    public Game(int id, League league, Team homeTeam, Team awayTeam) {
        this.id = id;
        this.league = league;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }
}
