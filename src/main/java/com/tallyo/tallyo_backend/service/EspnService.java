package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.config.EspnApiProperties;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.GameStat;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.mapper.EspnBoxScoreMapper;
import com.tallyo.tallyo_backend.mapper.EspnGameMapper;
import com.tallyo.tallyo_backend.mapper.EspnSummaryDetailsMapper;
import com.tallyo.tallyo_backend.model.espn.box_score.EspnBoxscoreResponse;
import com.tallyo.tallyo_backend.model.espn.scoreboard.EspnScoreboardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EspnService {
    private static final Logger logger = LoggerFactory.getLogger(EspnService.class);

    private final RestTemplate restTemplate;

    private final EspnApiProperties espnApiProperties;

    EspnGameMapper espnGameMapper = new EspnGameMapper();
    EspnBoxScoreMapper espnBoxScoreMapper = new EspnBoxScoreMapper();
    EspnSummaryDetailsMapper espnSummaryDetailsMapper = new EspnSummaryDetailsMapper();


    public EspnService(RestTemplate restTemplate, EspnApiProperties espnApiProperties) {
        this.restTemplate = restTemplate;
        this.espnApiProperties = espnApiProperties;
    }

    // ESPN's scoreboard endpoint now rejects the "dates=START-END" range syntax
    // outright (400 "Failed to get events endpoint.") even for a single-day
    // range like "X-X" -- only a bare "dates=X" is accepted. Query each day in
    // [startDate, endDate] separately and merge, deduping by game id in case a
    // game is returned from more than one day's scoreboard.
    public List<Game> fetchGames(League league, String startDate, String endDate, boolean shouldFetchStats) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);

        Map<Integer, Game> gamesById = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String gamesUrl = String.format("%s/%s/%s/scoreboard?limit=%d&dates=%s",
                    espnApiProperties.getBaseUrl(),
                    league.getSport().getValue(),
                    league.getValue(),
                    espnApiProperties.getScoreboard().getLimit(),
                    date.format(formatter));
            EspnScoreboardResponse espnScoreboardResponse = fetchGamesForUrl(gamesUrl);
            if (espnScoreboardResponse != null) {
                espnScoreboardResponse.getEvents().stream()
                        .map(event -> espnGameMapper.toGame(event, league))
                        .filter(Objects::nonNull)
                        .forEach(game -> gamesById.put(game.getId(), game));
            }
        }
        List<Game> games = new ArrayList<>(gamesById.values());
        if (shouldFetchStats) {
            games.forEach(game -> attachStatsToGame(game, league));
        }
        return games;
    }

    private void attachStatsToGame(Game game, League league) {
        try {
            EspnBoxscoreResponse summary = fetchSummaryForGame(game.getId(), league);
            if (summary == null) {
                return;
            }
            game.addStats(toGameStats(summary, game.getId()));
            game.addLeaders(espnSummaryDetailsMapper.toLeaders(summary, game.getId()));
            game.addScoringPlays(espnSummaryDetailsMapper.toScoringPlays(summary, league, game));
            if (league.isSupportsPlayerStats()) {
                game.addPlayers(espnSummaryDetailsMapper.toPlayers(summary, game.getId()));
            }
        } catch (Exception e) {
            logger.warn("Could not fetch stats for game {}: {}",
                    game.getId(), e.getMessage());
        }
    }

    public List<Game> fetchGames(League league, int year, boolean shouldFetchStats) {
        int resolvedYear = year == 0 ? Year.now().getValue() : year;
        List<Game> games = fetchGamesForRange(league,
                LocalDate.of(resolvedYear, 1, 1),
                LocalDate.of(resolvedYear, 12, 31));
        if (shouldFetchStats) {
            games.forEach(game -> attachStatsToGame(game, league));
        }
        return games;
    }

    // ESPN's scoreboard endpoint rejects "dates=START-END" range syntax outright
    // (see fetchGames(String, String, boolean) above), so a season backfill has
    // to walk day by day too. A single day never comes close to the 1000-event
    // cap that used to require bisecting, so that logic is no longer needed.
    private List<Game> fetchGamesForRange(League league, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        int limit = espnApiProperties.getScoreboard().getLimit();
        Map<Integer, Game> gamesById = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String gamesUrl = String.format("%s/%s/%s/scoreboard?limit=%d&dates=%s",
                    espnApiProperties.getBaseUrl(),
                    league.getSport().getValue(),
                    league.getValue(),
                    limit,
                    date.format(formatter));
            EspnScoreboardResponse espnScoreboardResponse = fetchGamesForUrl(gamesUrl);
            if (espnScoreboardResponse != null && espnScoreboardResponse.getEvents() != null) {
                espnScoreboardResponse.getEvents().stream()
                        .map(event -> espnGameMapper.toGame(event, league))
                        .filter(Objects::nonNull)
                        .forEach(game -> gamesById.put(game.getId(), game));
            }
        }
        return new ArrayList<>(gamesById.values());
    }

    public List<GameStat> fetchStatsForGame(int gameId, League league) {
        EspnBoxscoreResponse summary = fetchSummaryForGame(gameId, league);
        return summary != null ? toGameStats(summary, gameId) : new ArrayList<>();
    }

    private EspnBoxscoreResponse fetchSummaryForGame(int gameId, League league) {
        String summaryUrl = String.format("%s/%s/%s/summary?event=%s",
                espnApiProperties.getBaseUrl(),
                league.getSport().getValue(),
                league.getValue(),
                gameId);
        try {
            return restTemplate.getForObject(summaryUrl, EspnBoxscoreResponse.class);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    private List<GameStat> toGameStats(EspnBoxscoreResponse summary, int gameId) {
        if (summary.getBoxscore() == null || summary.getBoxscore().getTeams() == null) {
            return new ArrayList<>();
        }
        return summary.getBoxscore().getTeams().stream()
                .flatMap(team -> espnBoxScoreMapper.toGameStat(team, gameId).stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private EspnScoreboardResponse fetchGamesForUrl(String url) {
        try {
            return restTemplate.getForObject(url, EspnScoreboardResponse.class);
        } catch (Exception e) {
            logger.error("Failed to fetch/parse ESPN scoreboard from {}", url, e);
            throw e;
        }
    }
}




