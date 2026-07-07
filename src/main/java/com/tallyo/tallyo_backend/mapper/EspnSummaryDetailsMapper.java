package com.tallyo.tallyo_backend.mapper;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.GameLeader;
import com.tallyo.tallyo_backend.entity.GameLeaderKey;
import com.tallyo.tallyo_backend.entity.GamePlayer;
import com.tallyo.tallyo_backend.entity.GamePlayerKey;
import com.tallyo.tallyo_backend.entity.ScoringPlay;
import com.tallyo.tallyo_backend.entity.ScoringPlayKey;
import com.tallyo.tallyo_backend.entity.Team;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.enums.Sport;
import com.tallyo.tallyo_backend.model.espn.box_score.EspnBoxscoreResponse;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts stat leaders and scoring plays from ESPN summary payloads.
 * The summary shape varies per sport: football has top-level scoringPlays,
 * hockey and baseball have plays (filtered on scoringPlay=true), soccer has
 * keyEvents (filtered on scoringPlay=true). Leaders share one nesting across
 * sports; baseball summaries have no top-level leaders, so none are stored.
 */
public class EspnSummaryDetailsMapper {

    public List<GameLeader> toLeaders(EspnBoxscoreResponse summary, int gameId) {
        List<GameLeader> result = new ArrayList<>();
        JsonNode teamBlocks = summary.getLeaders();
        if (teamBlocks == null || !teamBlocks.isArray()) {
            return result;
        }
        for (JsonNode teamBlock : teamBlocks) {
            Integer teamId = intOrNull(teamBlock.path("team").path("id"));
            if (teamId == null) {
                continue;
            }
            for (JsonNode category : teamBlock.path("leaders")) {
                String statName = textOrNull(category.path("name"));
                JsonNode topLeader = category.path("leaders").path(0);
                if (statName == null || topLeader.isMissingNode()) {
                    continue;
                }
                result.add(GameLeader.builder()
                        .key(new GameLeaderKey(gameId, teamId, statName))
                        .displayName(textOrNull(category.path("displayName")))
                        .statValue(doubleOrNull(topLeader.path("value")))
                        .displayValue(textOrNull(topLeader.path("displayValue")))
                        .playerName(textOrNull(topLeader.path("athlete").path("fullName")))
                        .playerShortName(textOrNull(topLeader.path("athlete").path("shortName")))
                        .build());
            }
        }
        return result;
    }

    // Parses boxscore.players: per team, per stat group (batting/pitching), a
    // labels array plus one stats array per athlete. Live/final games carry game
    // stats; scheduled games carry the projected lineup with season stats. Both
    // are stored verbatim — the columns differ, so labels are stored per row.
    public List<GamePlayer> toPlayers(EspnBoxscoreResponse summary, int gameId) {
        List<GamePlayer> result = new ArrayList<>();
        if (summary.getBoxscore() == null) {
            return result;
        }
        JsonNode teamBlocks = summary.getBoxscore().getPlayers();
        if (teamBlocks == null || !teamBlocks.isArray()) {
            return result;
        }
        for (JsonNode teamBlock : teamBlocks) {
            Integer teamId = intOrNull(teamBlock.path("team").path("id"));
            if (teamId == null) {
                continue;
            }
            for (JsonNode statGroup : teamBlock.path("statistics")) {
                String category = textOrNull(statGroup.path("type"));
                String labels = joinValues(statGroup.path("labels"));
                if (category == null || labels == null) {
                    continue;
                }
                int displayOrder = 0;
                for (JsonNode athleteEntry : statGroup.path("athletes")) {
                    String playerId = textOrNull(athleteEntry.path("athlete").path("id"));
                    String stats = joinValues(athleteEntry.path("stats"));
                    if (playerId == null || stats == null) {
                        continue;
                    }
                    JsonNode athlete = athleteEntry.path("athlete");
                    result.add(GamePlayer.builder()
                            .key(new GamePlayerKey(gameId, teamId, playerId, category))
                            .playerName(fullName(athlete))
                            .playerShortName(shortName(athlete))
                            .position(textOrNull(athleteEntry.path("position").path("abbreviation")))
                            .batOrder(intOrNull(athleteEntry.path("batOrder")))
                            .starter(athleteEntry.path("starter").asBoolean(false))
                            .displayOrder(displayOrder++)
                            .statLabels(labels)
                            .statValues(stats)
                            .build());
                }
            }
        }
        return result;
    }

    // Baseball/hockey athletes carry fullName/shortName; football only has
    // firstName/lastName/displayName, so fall back and derive "F. Last".
    private String fullName(JsonNode athlete) {
        String fullName = textOrNull(athlete.path("fullName"));
        return fullName != null ? fullName : textOrNull(athlete.path("displayName"));
    }

    private String shortName(JsonNode athlete) {
        String shortName = textOrNull(athlete.path("shortName"));
        if (shortName != null) {
            return shortName;
        }
        String firstName = textOrNull(athlete.path("firstName"));
        String lastName = textOrNull(athlete.path("lastName"));
        if (firstName != null && lastName != null) {
            return firstName.charAt(0) + ". " + lastName;
        }
        return textOrNull(athlete.path("displayName"));
    }

    private String joinValues(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : arrayNode) {
            values.add(value.isValueNode() ? value.asText() : "");
        }
        return String.join("|", values);
    }

    public List<ScoringPlay> toScoringPlays(EspnBoxscoreResponse summary, League league, Game game) {
        List<ScoringPlay> result = new ArrayList<>();
        JsonNode source = scoringPlaySource(summary, league);
        if (source == null || !source.isArray()) {
            return result;
        }
        int sequence = 0;
        Set<String> seenAtBatScores = new HashSet<>();
        for (JsonNode play : source) {
            // Football payloads list only scoring plays; hockey/soccer/baseball
            // list all plays/events and flag the scoring ones.
            if (league.getSport() != Sport.FOOTBALL && !play.path("scoringPlay").asBoolean(false)) {
                continue;
            }
            // Baseball flags both the event play (e.g. Stolen Base) and the
            // at-bat's Play Result for the same run; keep one per at-bat score
            // state so distinct runs within an at-bat still both survive.
            String atBatId = textOrNull(play.path("atBatId"));
            if (atBatId != null) {
                String dedupeKey = atBatId + "|" + intOrNull(play.path("homeScore"))
                        + "|" + intOrNull(play.path("awayScore"));
                if (!seenAtBatScores.add(dedupeKey)) {
                    continue;
                }
            }
            String playId = textOrNull(play.path("id"));
            if (playId == null) {
                playId = "seq-" + sequence;
            }
            String teamId = textOrNull(play.path("team").path("id"));
            result.add(ScoringPlay.builder()
                    .key(new ScoringPlayKey(game.getId(), playId))
                    .teamId(teamId)
                    .teamAbbreviation(resolveTeamAbbreviation(play, teamId, game))
                    .displayText(textOrNull(play.path("text")))
                    .homeScore(intOrNull(play.path("homeScore")))
                    .awayScore(intOrNull(play.path("awayScore")))
                    .scoringType(resolveScoringType(play))
                    .period(intOrNull(play.path("period").path("number")))
                    .clock(textOrNull(play.path("clock").path("displayValue")))
                    .sequence(sequence++)
                    .build());
        }
        return result;
    }

    private JsonNode scoringPlaySource(EspnBoxscoreResponse summary, League league) {
        return switch (league.getSport()) {
            case FOOTBALL -> summary.getScoringPlays();
            case HOCKEY, BASEBALL -> summary.getPlays();
            case SOCCER -> summary.getKeyEvents();
        };
    }

    private String resolveScoringType(JsonNode play) {
        String fromScoringType = textOrNull(play.path("scoringType").path("abbreviation"));
        if (fromScoringType != null) {
            return fromScoringType;
        }
        String fromTypeAbbreviation = textOrNull(play.path("type").path("abbreviation"));
        if (fromTypeAbbreviation != null) {
            return fromTypeAbbreviation;
        }
        return textOrNull(play.path("type").path("text"));
    }

    private String resolveTeamAbbreviation(JsonNode play, String teamId, Game game) {
        String fromPlay = textOrNull(play.path("team").path("abbreviation"));
        if (fromPlay != null) {
            return fromPlay;
        }
        if (teamId == null) {
            return null;
        }
        Team home = game.getHomeTeam();
        if (home != null && home.getTeamKey() != null
                && teamId.equals(String.valueOf(home.getTeamKey().getTeamId()))) {
            return home.getAbbreviation();
        }
        Team away = game.getAwayTeam();
        if (away != null && away.getTeamKey() != null
                && teamId.equals(String.valueOf(away.getTeamKey().getTeamId()))) {
            return away.getAbbreviation();
        }
        return null;
    }

    private String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull() || !node.isValueNode()) {
            return null;
        }
        String text = node.asText();
        return text.isEmpty() ? null : text;
    }

    private Integer intOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull() || !node.isValueNode()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        try {
            return Integer.parseInt(node.asText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double doubleOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.asDouble();
    }
}
