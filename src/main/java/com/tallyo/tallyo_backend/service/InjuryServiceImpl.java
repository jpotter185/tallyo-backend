package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.config.EspnApiProperties;
import com.tallyo.tallyo_backend.dto.InjuredPlayerResponse;
import com.tallyo.tallyo_backend.dto.InjuryReportResponse;
import com.tallyo.tallyo_backend.dto.QbDepthChartEntryResponse;
import com.tallyo.tallyo_backend.dto.TeamInjuryReportResponse;
import com.tallyo.tallyo_backend.entity.QbDepthChartEntry;
import com.tallyo.tallyo_backend.entity.QbDepthChartKey;
import com.tallyo.tallyo_backend.entity.TeamInjury;
import com.tallyo.tallyo_backend.entity.TeamInjuryKey;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.exception.InvalidRequestException;
import com.tallyo.tallyo_backend.repository.QbDepthChartRepository;
import com.tallyo.tallyo_backend.repository.TeamInjuryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Current NFL injury designations and QB depth charts, refreshed from ESPN.
 *
 * Sources (per team): the site API roster, which carries every player's current
 * injury status (the game summary's injury list is capped at 5 per team, and the
 * league-wide feed at 25), and the core API depth chart for QB order. ESPN only
 * exposes the current state, so there is no history.
 */
@Service
public class InjuryServiceImpl implements InjuryService {

    private static final Logger logger = LoggerFactory.getLogger(InjuryServiceImpl.class);
    private static final Set<League> SUPPORTED = EnumSet.of(League.NFL);
    private static final Pattern ATHLETE_ID = Pattern.compile("/athletes/(\\d+)");

    private final RestTemplate restTemplate;
    private final EspnApiProperties espnApiProperties;
    private final TeamInjuryRepository injuryRepository;
    private final QbDepthChartRepository depthChartRepository;
    private final TransactionTemplate transactionTemplate;

    public InjuryServiceImpl(RestTemplate restTemplate,
                             EspnApiProperties espnApiProperties,
                             TeamInjuryRepository injuryRepository,
                             QbDepthChartRepository depthChartRepository,
                             TransactionTemplate transactionTemplate) {
        this.restTemplate = restTemplate;
        this.espnApiProperties = espnApiProperties;
        this.injuryRepository = injuryRepository;
        this.depthChartRepository = depthChartRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refreshAllSafely();
    }

    // Designations change through the week (official game statuses land Friday),
    // so refresh a few times a day rather than once.
    @Scheduled(cron = "0 15 */6 * * *", zone = "America/New_York")
    public void refreshScheduled() {
        refreshAllSafely();
    }

    // An exception escaping an ApplicationReadyEvent listener would fail startup, so an
    // ESPN outage must never propagate out of here.
    private void refreshAllSafely() {
        for (League league : SUPPORTED) {
            try {
                refreshInjuries(league);
            } catch (Exception e) {
                logger.error("Injury refresh failed for {}: {}", league, e.getMessage());
            }
        }
    }

    @Override
    public int refreshInjuries(League league) {
        requireSupported(league);
        long start = System.currentTimeMillis();
        int refreshed = 0;
        JsonNode teams = restTemplate.getForObject(teamsUrl(league), JsonNode.class);
        if (teams == null) {
            throw new IllegalStateException("ESPN returned no team list");
        }
        for (JsonNode entry : teams.path("sports").path(0).path("leagues").path(0).path("teams")) {
            JsonNode team = entry.path("team");
            Integer teamId = intOrNull(team.path("id"));
            if (teamId == null) {
                continue;
            }
            try {
                refreshTeam(league, teamId, textOrNull(team.path("abbreviation")));
                refreshed++;
            } catch (Exception e) {
                // Keep the team's previous rows rather than wiping them on a bad fetch.
                logger.warn("Injury refresh failed for {} team {}: {}", league, teamId, e.getMessage());
            }
        }
        logger.info("Refreshed injuries for {}: {} teams in {}ms", league, refreshed,
                System.currentTimeMillis() - start);
        return refreshed;
    }

    private void refreshTeam(League league, int teamId, String abbreviation) {
        JsonNode roster = restTemplate.getForObject(rosterUrl(league, teamId), JsonNode.class);
        Integer season = intOrNull(roster.path("season").path("year"));
        JsonNode depthCharts = restTemplate.getForObject(
                depthChartUrl(league, season != null ? season : Year.now().getValue(), teamId), JsonNode.class);
        Instant now = Instant.now();

        Map<String, String> names = new HashMap<>();
        List<TeamInjury> injuries = new ArrayList<>();
        for (JsonNode group : roster.path("athletes")) {
            for (JsonNode athlete : group.path("items")) {
                String athleteId = textOrNull(athlete.path("id"));
                if (athleteId == null) {
                    continue;
                }
                names.put(athleteId, textOrNull(athlete.path("displayName")));
                JsonNode injury = athlete.path("injuries").path(0);
                String status = textOrNull(injury.path("status"));
                if (status == null) {
                    continue;
                }
                injuries.add(TeamInjury.builder()
                        .key(new TeamInjuryKey(league, teamId, athleteId))
                        .teamAbbreviation(abbreviation)
                        .athleteName(names.get(athleteId))
                        .position(textOrNull(athlete.path("position").path("abbreviation")))
                        .status(status)
                        .statusDate(instantOrNull(injury.path("date")))
                        .updatedAt(now)
                        .build());
            }
        }
        if (names.isEmpty()) {
            throw new IllegalStateException("roster had no athletes");
        }

        List<QbDepthChartEntry> qbs = new ArrayList<>();
        for (JsonNode chart : depthCharts.path("items")) {
            JsonNode qbDepth = chart.path("positions").path("qb").path("athletes");
            if (!qbDepth.isArray() || qbDepth.isEmpty()) {
                continue;
            }
            for (JsonNode entry : qbDepth) {
                Integer rank = intOrNull(entry.path("rank"));
                Matcher matcher = ATHLETE_ID.matcher(Objects.toString(textOrNull(entry.path("athlete").path("$ref")), ""));
                if (rank == null || !matcher.find()) {
                    continue;
                }
                String athleteId = matcher.group(1);
                qbs.add(QbDepthChartEntry.builder()
                        .key(new QbDepthChartKey(league, teamId, rank))
                        .teamAbbreviation(abbreviation)
                        .athleteId(athleteId)
                        .athleteName(names.get(athleteId))
                        .updatedAt(now)
                        .build());
            }
            break;
        }

        transactionTemplate.executeWithoutResult(status -> {
            injuryRepository.deleteByTeam(league, teamId);
            injuryRepository.saveAll(injuries);
            depthChartRepository.deleteByTeam(league, teamId);
            depthChartRepository.saveAll(qbs);
        });
    }

    @Override
    public InjuryReportResponse getInjuries(League league) {
        requireSupported(league);
        Map<Integer, List<TeamInjury>> injuriesByTeam = injuryRepository.findByLeague(league).stream()
                .collect(Collectors.groupingBy(i -> i.getKey().getTeamId(), TreeMap::new, Collectors.toList()));
        Map<Integer, List<QbDepthChartEntry>> depthByTeam = depthChartRepository.findByLeague(league).stream()
                .collect(Collectors.groupingBy(q -> q.getKey().getTeamId(), TreeMap::new, Collectors.toList()));

        SortedSet<Integer> teamIds = new TreeSet<>(injuriesByTeam.keySet());
        teamIds.addAll(depthByTeam.keySet());
        List<TeamInjuryReportResponse> teams = new ArrayList<>();
        Instant oldest = null;
        for (Integer teamId : teamIds) {
            List<TeamInjury> injuries = injuriesByTeam.getOrDefault(teamId, List.of());
            List<QbDepthChartEntry> depth = depthByTeam.getOrDefault(teamId, List.of());
            Map<String, String> statusByAthlete = injuries.stream()
                    .collect(Collectors.toMap(i -> i.getKey().getAthleteId(), TeamInjury::getStatus, (a, b) -> a));

            Instant updatedAt = injuries.isEmpty() ? depth.get(0).getUpdatedAt() : injuries.get(0).getUpdatedAt();
            if (oldest == null || updatedAt.isBefore(oldest)) {
                oldest = updatedAt;
            }
            String abbreviation = injuries.isEmpty() ? depth.get(0).getTeamAbbreviation() : injuries.get(0).getTeamAbbreviation();
            teams.add(TeamInjuryReportResponse.builder()
                    .teamId(teamId)
                    .abbreviation(abbreviation)
                    .updatedAt(updatedAt.toString())
                    .injuries(injuries.stream().map(i -> InjuredPlayerResponse.builder()
                            .athleteId(i.getKey().getAthleteId())
                            .name(i.getAthleteName())
                            .position(i.getPosition())
                            .status(i.getStatus())
                            .statusDate(i.getStatusDate() == null ? null : i.getStatusDate().toString())
                            .build()).toList())
                    .qbDepthChart(depth.stream().map(q -> QbDepthChartEntryResponse.builder()
                            .rank(q.getKey().getDepthRank())
                            .athleteId(q.getAthleteId())
                            .name(q.getAthleteName())
                            .injuryStatus(statusByAthlete.get(q.getAthleteId()))
                            .build()).toList())
                    .build());
        }
        return InjuryReportResponse.builder()
                .league(league.getId())
                .updatedAt(oldest == null ? null : oldest.toString())
                .teams(teams)
                .build();
    }

    private void requireSupported(League league) {
        if (!SUPPORTED.contains(league)) {
            throw new InvalidRequestException("Injuries not supported for league: " + league.getId());
        }
    }

    private String teamsUrl(League league) {
        return String.format("%s/%s/%s/teams",
                espnApiProperties.getBaseUrl(), league.getSport().getValue(), league.getValue());
    }

    private String rosterUrl(League league, int teamId) {
        return String.format("%s/%s/%s/teams/%d/roster",
                espnApiProperties.getBaseUrl(), league.getSport().getValue(), league.getValue(), teamId);
    }

    private String depthChartUrl(League league, int season, int teamId) {
        return String.format("%s/%s/leagues/%s/seasons/%d/teams/%d/depthcharts",
                espnApiProperties.getCoreBaseUrl(), league.getSport().getValue(), league.getValue(), season, teamId);
    }

    private static String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull() || !node.isValueNode()) {
            return null;
        }
        String text = node.asText();
        return text.isEmpty() ? null : text;
    }

    private static Integer intOrNull(JsonNode node) {
        String text = textOrNull(node);
        if (text == null) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ESPN omits seconds ("2026-09-22T22:16Z"), which Instant.parse rejects.
    private static Instant instantOrNull(JsonNode node) {
        String text = textOrNull(node);
        if (text == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
