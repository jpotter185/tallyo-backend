package com.tallyo.tallyo_backend.mapper;

import com.tallyo.tallyo_backend.entity.GameStat;
import com.tallyo.tallyo_backend.entity.GameStatKey;
import com.tallyo.tallyo_backend.model.espn.box_score.Statistic;
import com.tallyo.tallyo_backend.model.espn.box_score.Team;

import java.util.ArrayList;
import java.util.List;

public class EspnBoxScoreMapper {

    public List<GameStat> toGameStat(Team team, int gameId){
        List<GameStat> gameStats = new ArrayList<>();

        for(Statistic stat: team.getStatistics()){
            GameStatKey key = new GameStatKey(gameId, team.getTeam().getId(), stat.getName());

            GameStat gameStat = GameStat.builder()
                    .key(key)
                    .statValue(stat.getDisplayValue())
                    .build();
            gameStats.add(gameStat);
        }
        return gameStats;
    }
}
