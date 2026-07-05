package com.tallyo.tallyo_backend.mapper;

import tools.jackson.databind.JsonNode;
import com.tallyo.tallyo_backend.dto.StandingsGroupResponse;
import com.tallyo.tallyo_backend.dto.StandingsTeamResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps ESPN standings payloads into standings groups. ESPN's response shape
 * varies by endpoint (cdn.espn.com xhr payloads vs site.api.espn.com v2), so
 * this walks the JSON leniently rather than binding to typed POJOs. Port of
 * the frontend's standingsMapper.ts.
 */
@Component
public class EspnStandingsMapper {

    private static final String FALLBACK_LOGO =
            "https://a.espncdn.com/i/teamlogos/leagues/500/nhl.png";

    public List<StandingsGroupResponse> map(JsonNode payload) {
        List<StandingsGroupResponse> standings = new ArrayList<>();
        for (JsonNode group : getConferenceGroups(payload)) {
            StandingsGroupResponse parsed = parseConference(group);
            if (!parsed.getTeams().isEmpty()) {
                standings.add(parsed);
            }
        }
        return standings;
    }

    private List<JsonNode> getConferenceGroups(JsonNode payload) {
        if (payload == null || payload.isMissingNode()) {
            return List.of();
        }
        JsonNode directGroups = payload.path("content").path("standings").path("groups");
        if (directGroups.isArray()) {
            return toList(directGroups);
        }
        JsonNode conferenceChildren = payload.path("children");
        if (conferenceChildren.isArray()) {
            List<JsonNode> conferences = toList(conferenceChildren).stream()
                    .filter(this::hasStandingsGroupShape)
                    .toList();
            if (!conferences.isEmpty()) {
                return conferences;
            }
        }

        // Endpoint shapes vary; walk the payload and keep only groups that
        // contain standings entries directly or in nested groups.
        List<JsonNode> candidateGroups = new ArrayList<>();
        Set<JsonNode> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        walk(payload, candidateGroups, visited);
        return candidateGroups;
    }

    private void walk(JsonNode node, List<JsonNode> candidates, Set<JsonNode> visited) {
        if (node == null || !node.isContainer() || visited.contains(node)) {
            return;
        }
        visited.add(node);

        for (String field : new String[]{"groups", "children", "items"}) {
            JsonNode collection = node.path(field);
            if (!collection.isArray()) {
                continue;
            }
            for (JsonNode group : collection) {
                if (hasStandingsGroupShape(group)) {
                    candidates.add(group);
                }
            }
        }

        for (JsonNode value : node) {
            walk(value, candidates, visited);
        }
    }

    private boolean hasStandingsGroupShape(JsonNode group) {
        return !extractStandingEntries(group).isEmpty()
                || group.path("groups").isArray()
                || group.path("children").isArray()
                || group.path("items").isArray();
    }

    private List<JsonNode> extractStandingEntries(JsonNode group) {
        JsonNode nestedEntries = group.path("standings").path("entries");
        if (nestedEntries.isArray()) {
            return toList(nestedEntries);
        }
        JsonNode entries = group.path("entries");
        if (entries.isArray()) {
            return toList(entries);
        }
        return List.of();
    }

    private record EntryWithDivision(JsonNode entry, String divisionName) {
    }

    private List<EntryWithDivision> collectEntriesFromGroup(JsonNode group, String fallbackDivisionName) {
        String divisionName = textOrDefault(group.path("name"), fallbackDivisionName);
        List<JsonNode> entries = extractStandingEntries(group);
        if (!entries.isEmpty()) {
            return entries.stream()
                    .map(entry -> new EntryWithDivision(entry, divisionName))
                    .toList();
        }

        List<EntryWithDivision> collected = new ArrayList<>();
        for (String field : new String[]{"groups", "children", "items"}) {
            JsonNode nested = group.path(field);
            if (!nested.isArray()) {
                continue;
            }
            for (JsonNode child : nested) {
                collected.addAll(collectEntriesFromGroup(child, divisionName));
            }
        }
        return collected;
    }

    private StandingsGroupResponse parseConference(JsonNode conferenceGroup) {
        String groupName = textOrDefault(conferenceGroup.path("name"), "Standings");
        List<StandingsTeamResponse> teams = new ArrayList<>();
        for (EntryWithDivision entryInfo : collectEntriesFromGroup(conferenceGroup, null)) {
            StandingsTeamResponse team = parseTeam(entryInfo.entry(), groupName, entryInfo.divisionName());
            if (team != null) {
                teams.add(team);
            }
        }
        teams.sort(Comparator.comparingInt(this::sortRank));
        return StandingsGroupResponse.builder()
                .groupName(groupName)
                .teams(teams)
                .build();
    }

    // Soccer payloads (e.g. World Cup groups) arrive unsorted but carry a
    // "rank" stat; other leagues either pre-sort entries or expose a seed.
    private int sortRank(StandingsTeamResponse team) {
        String rank = team.getStats() == null ? null : team.getStats().get("rank");
        int parsedRank = parseLeadingInt(rank, 0);
        if (parsedRank > 0) {
            return parsedRank;
        }
        return parseLeadingInt(team.getSeed(), 100);
    }

    private StandingsTeamResponse parseTeam(JsonNode standingEntry, String conferenceName, String divisionName) {
        JsonNode teamInfo = standingEntry.path("team");
        if (teamInfo.isMissingNode() || teamInfo.isNull()) {
            return null;
        }

        Map<String, String> stats = new LinkedHashMap<>();
        for (JsonNode stat : standingEntry.path("stats")) {
            String type = textOrDefault(stat.path("type"), null);
            String displayValue = textOrDefault(stat.path("displayValue"), null);
            if (type != null && displayValue != null) {
                stats.put(type, displayValue);
            }
        }

        String record = stats.get("record");
        String wins = stats.get("wins");
        String losses = stats.get("losses");
        String ties = stats.get("ties");
        if (isTruthy(wins) && isTruthy(losses) && isTruthy(ties)) {
            record = wins + "-" + losses + "-" + ties;
        }

        String differential = stats.get("differential");
        if (!isTruthy(differential)) {
            int computed = parseLeadingInt(stats.get("pointsfor"), 0)
                    - parseLeadingInt(stats.get("pointsagainst"), 0);
            differential = computed != 0 ? String.valueOf(computed) : differential;
        }

        String logo = textOrDefault(teamInfo.path("logos").path(0).path("href"), FALLBACK_LOGO);
        String name = textOrDefault(teamInfo.path("displayName"),
                textOrDefault(teamInfo.path("name"), "Unknown Team"));

        return StandingsTeamResponse.builder()
                .id(textOrDefault(teamInfo.path("id"), ""))
                .name(name)
                .abbreviation(textOrDefault(teamInfo.path("abbreviation"), ""))
                .logo(logo)
                .location(textOrDefault(teamInfo.path("location"), ""))
                .seed(textOrDefault(teamInfo.path("seed"), null))
                .conference(conferenceName)
                .division(divisionName)
                .record(record)
                .differential(differential)
                .stats(stats)
                .build();
    }

    private List<JsonNode> toList(JsonNode array) {
        List<JsonNode> list = new ArrayList<>(array.size());
        array.forEach(list::add);
        return list;
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        if (node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        return node.isValueNode() ? node.asText() : defaultValue;
    }

    private boolean isTruthy(String value) {
        return value != null && !value.isEmpty();
    }

    private int parseLeadingInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        int end = 0;
        if (end < trimmed.length() && (trimmed.charAt(end) == '-' || trimmed.charAt(end) == '+')) {
            end++;
        }
        while (end < trimmed.length() && Character.isDigit(trimmed.charAt(end))) {
            end++;
        }
        try {
            return Integer.parseInt(trimmed.substring(0, end));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
