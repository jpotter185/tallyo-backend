package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.config.EspnApiProperties;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.mapper.EspnGameMapper;
import com.tallyo.tallyo_backend.model.espn.EspnScoreboardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class EspnService {
    private static final Logger logger = LoggerFactory.getLogger(EspnService.class);

    private final RestTemplate restTemplate;

    private final EspnApiProperties espnApiProperties;


    public EspnService(RestTemplate restTemplate, EspnApiProperties espnApiProperties) {
        this.restTemplate = restTemplate;
        this.espnApiProperties = espnApiProperties;
    }

    public List<Game> fetchGames(League league, int year) {
        year = year == 0 ? Year.now().getValue() : year;
        String gamesUrl = String.format("%s/%s/scoreboard?limit=%d&dates=%d0101-%d1231",
                espnApiProperties.getBaseUrl(),
                league.getValue(),
                espnApiProperties.getScoreboard().getLimit(),
                year,
                year);
        logger.info("Calling " + gamesUrl);
        EspnScoreboardResponse espnScoreboardResponse = restTemplate.getForObject(gamesUrl, EspnScoreboardResponse.class);
        logger.info("Got response from ESPN");
        EspnGameMapper espnGameMapper= new EspnGameMapper();
        List<Game> games = new ArrayList<>();
        if(espnScoreboardResponse != null){
            games = espnScoreboardResponse.getEvents().stream()
                    .map(event -> espnGameMapper.toGame(event, league))
                    .filter(Objects::nonNull)
                    .toList();

        }
        return games;
    }
}




