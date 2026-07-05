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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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

    public List<Game> fetchGames(League league, String startDate, String endDate, boolean shouldFetchStats) {
        String gamesUrl = String.format("%s/%s/%s/scoreboard?limit=%d&dates=%s-%s",
                espnApiProperties.getBaseUrl(),
                league.getSport().getValue(),
                league.getValue(),
                espnApiProperties.getScoreboard().getLimit(),
                startDate,
                endDate);
        EspnScoreboardResponse espnScoreboardResponse = fetchGamesForUrl(gamesUrl);
        List<Game> games = new ArrayList<>();
        if (espnScoreboardResponse != null) {
            games = espnScoreboardResponse.getEvents().stream()
                    .map(event -> espnGameMapper.toGame(event, league))
                    .filter(Objects::nonNull)
                    .toList();
        }
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

    // ESPN silently caps scoreboard responses at 1000 events (larger limits fall
    // back to a 25-event default), which truncates season-long fetches for
    // high-volume leagues like MLB (~2900 games/year). When a response fills the
    // limit, bisect the date range and re-fetch each half.
    private List<Game> fetchGamesForRange(League league, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        int limit = espnApiProperties.getScoreboard().getLimit();
        String gamesUrl = String.format("%s/%s/%s/scoreboard?limit=%d&dates=%s-%s",
                espnApiProperties.getBaseUrl(),
                league.getSport().getValue(),
                league.getValue(),
                limit,
                startDate.format(formatter),
                endDate.format(formatter));
        EspnScoreboardResponse espnScoreboardResponse = fetchGamesForUrl(gamesUrl);
        if (espnScoreboardResponse == null || espnScoreboardResponse.getEvents() == null) {
            return new ArrayList<>();
        }
        if (espnScoreboardResponse.getEvents().size() >= limit && startDate.isBefore(endDate)) {
            LocalDate midDate = startDate.plusDays(ChronoUnit.DAYS.between(startDate, endDate) / 2);
            List<Game> games = new ArrayList<>(fetchGamesForRange(league, startDate, midDate));
            games.addAll(fetchGamesForRange(league, midDate.plusDays(1), endDate));
            return games;
        }
        return espnScoreboardResponse.getEvents().stream()
                .map(event -> espnGameMapper.toGame(event, league))
                .filter(Objects::nonNull)
                .toList();
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




