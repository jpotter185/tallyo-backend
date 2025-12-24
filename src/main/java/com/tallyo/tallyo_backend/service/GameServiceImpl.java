package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameServiceImpl implements GameService{

    private final GameRepository gameRepository;
    private final EspnService espnService;
    public GameServiceImpl(GameRepository gameRepository, EspnService espnService){
        this.gameRepository = gameRepository;
        this.espnService = espnService;
    }

    @Override
    public List<Game> getGames(League league, int year, int seasonType, int week) {
        return gameRepository.getGames(league, year, seasonType, week);
    }

    @Override
    public List<Game> updateGames(League league, int year) {
        List<Game> games = espnService.fetchGames(league, year);
        gameRepository.saveAll(games);
        System.out.println("Got " + games.size() + " games from ESPN API");
        return games;
    }
}
