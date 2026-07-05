package com.tallyo.tallyo_backend.mapper;

import com.tallyo.tallyo_backend.entity.GameStat;
import com.tallyo.tallyo_backend.entity.GameStatKey;
import com.tallyo.tallyo_backend.model.espn.box_score.Statistic;
import com.tallyo.tallyo_backend.model.espn.box_score.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EspnBoxScoreMapper {

    // Baseball summaries nest team stats in groups (batting/pitching/fielding)
    // and carry ~60 stats per group. The 20s live-update job re-saves stats for
    // every in-progress game, so only a dashboard-sized subset is kept. Stored
    // keys are "<group>.<name>" because names repeat across groups (e.g. hits).
    private static final Map<String, Set<String>> GROUPED_STAT_WHITELIST = Map.of(
            "batting", Set.of("runs", "hits", "homeRuns", "RBIs", "avg", "strikeouts", "walks",
                    "stolenBases", "doubles", "triples", "atBats", "runnersLeftOnBase"),
            "pitching", Set.of("ERA", "earnedRuns", "strikeouts", "walks", "hits", "homeRuns", "pitches"),
            "fielding", Set.of("errors")
    );

    public List<GameStat> toGameStat(Team team, int gameId) {
        List<GameStat> gameStats = new ArrayList<>();

        for (Statistic stat : team.getStatistics()) {
            if (stat.getStats() != null) {
                gameStats.addAll(toGroupedGameStats(stat, team, gameId));
                continue;
            }
            GameStatKey key = new GameStatKey(gameId, team.getTeam().getId(), stat.getName());

            GameStat gameStat = GameStat.builder()
                    .key(key)
                    .statValue(stat.getDisplayValue())
                    .build();
            gameStats.add(gameStat);
        }
        return gameStats;
    }

    private List<GameStat> toGroupedGameStats(Statistic group, Team team, int gameId) {
        List<GameStat> gameStats = new ArrayList<>();
        Set<String> allowedNames = GROUPED_STAT_WHITELIST.getOrDefault(group.getName(), Set.of());
        for (Statistic stat : group.getStats()) {
            if (!allowedNames.contains(stat.getName())) {
                continue;
            }
            GameStatKey key = new GameStatKey(gameId, team.getTeam().getId(),
                    group.getName() + "." + stat.getName());
            gameStats.add(GameStat.builder()
                    .key(key)
                    .statValue(stat.getDisplayValue())
                    .build());
        }
        return gameStats;
    }
}
