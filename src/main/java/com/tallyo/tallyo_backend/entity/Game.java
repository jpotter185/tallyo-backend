package com.tallyo.tallyo_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tallyo.tallyo_backend.dto.StatResponse;
import com.tallyo.tallyo_backend.enums.League;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @OneToOne(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private GameOdd gameOdd;
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "key")
    @JsonIgnore
    private Map<GameStatKey, GameStat> stats = new HashMap<>();
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

    public void addStats(List<GameStat> newStats) {
        if (this.stats == null) {
            this.stats = new HashMap<>();
        }
        if (newStats != null) {
            newStats.forEach(stat -> {
                this.stats.put(stat.getKey(), stat);
                stat.setGame(this);
            });
        }
    }

    @JsonProperty("stats")
    public StatResponse getStatsForJson() {
        if (stats == null) return null;
        Map<String,String> homeStats = new HashMap<>();
        Map<String,String> awayStats = new HashMap<>();
        for( Map.Entry<GameStatKey, GameStat> entry: this.getStats().entrySet()){
            String statName = entry.getKey().getStatType();
            String statValue = entry.getValue().getStatValue();
            if(entry.getKey().getTeamId() == this.getHomeTeam().getTeamKey().getTeamId()){homeStats.put(statName, statValue);
            }
            else {
                awayStats.put(statName, statValue);
            }
        }
        return new StatResponse(homeStats, awayStats);
    }
}
