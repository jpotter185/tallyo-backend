package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.config.EspnApiProperties;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.GameStat;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.mapper.EspnBoxScoreMapper;
import com.tallyo.tallyo_backend.mapper.EspnGameMapper;
import com.tallyo.tallyo_backend.model.espn.box_score.EspnBoxscoreResponse;
import com.tallyo.tallyo_backend.model.espn.scoreboard.EspnScoreboardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Year;
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
            List<GameStat> stats = fetchStatsForGame(
                    game.getId(),
                    league
            );
            game.addStats(stats);
        } catch (Exception e) {
            logger.warn("Could not fetch stats for game {}: {}",
                    game.getId(), e.getMessage());
        }
    }

    public List<Game> fetchGames(League league, int year, boolean shouldFetchStats) {
        year = year == 0 ? Year.now().getValue() : year;
        String gamesUrl = String.format("%s/%s/%s/scoreboard?limit=%d&dates=%d0101-%d1231",
                espnApiProperties.getBaseUrl(),
                league.getSport().getValue(),
                league.getValue(),
                espnApiProperties.getScoreboard().getLimit(),
                year,
                year);
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

    public List<GameStat> fetchStatsForGame(int gameId, League league) {

        String boxScoreUrl = String.format("%s/%s/%s/summary?event=%s",
                espnApiProperties.getBaseUrl(),
                league.getSport().getValue(),
                league.getValue(),
                gameId);
        List<GameStat> gameStats = new ArrayList<>();
        try {
            EspnBoxscoreResponse espnBoxscoreResponse = restTemplate.getForObject(boxScoreUrl, EspnBoxscoreResponse.class);

            if (espnBoxscoreResponse != null &&
                    espnBoxscoreResponse.getBoxscore() != null &&
                    espnBoxscoreResponse.getBoxscore().getTeams() != null) {

                gameStats = espnBoxscoreResponse.getBoxscore().getTeams().stream()
                        .flatMap(team -> espnBoxScoreMapper.toGameStat(team, gameId).stream())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }


        return gameStats;
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




