package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.mapper.EspnGameMapper;
import com.tallyo.tallyo_backend.model.espn.EspnScoreboardResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class EspnService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;


    public EspnService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public List<Game> fetchGames(League league, int year) {
        year = year == 0 ? Year.now().getValue() : year;
        String gamesUrl = "https://site.api.espn.com/apis/site/v2/sports/football/"
                + league.getValue() + "/scoreboard?limit=1000&dates=" + year + "0101-" + year + "1231";
        System.out.println("Calling " + gamesUrl);
        EspnScoreboardResponse espnScoreboardResponse = restTemplate.getForObject(gamesUrl, EspnScoreboardResponse.class);
        System.out.println("Got response from ESPN");
        System.out.println(espnScoreboardResponse);
        EspnGameMapper espnGameMapper= new EspnGameMapper();
        List<Game> games = new ArrayList<>();
        if(espnScoreboardResponse != null){
            games = espnScoreboardResponse.getEvents().stream()
                    .map(event -> espnGameMapper.toGame(event, League.NFL))
                    .filter(Objects::nonNull)
                    .toList();

        }
        return games;
    }
}




