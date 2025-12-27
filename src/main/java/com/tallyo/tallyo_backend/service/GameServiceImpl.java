package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameServiceImpl implements GameService{

    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

    private final GameRepository gameRepository;
    private final EspnService espnService;
    public GameServiceImpl(GameRepository gameRepository, EspnService espnService){
        this.gameRepository = gameRepository;
        this.espnService = espnService;
    }

    @Override
    public Page<Game> getGames(League league, int year, int seasonType, int week, Pageable pageable) {
        return gameRepository.getGames(league, year, seasonType, week, pageable);
    }

    @Override
    public List<Game> updateGames(League league, int year) {
        List<Game> games = espnService.fetchGames(league, year);
        gameRepository.saveAll(games);
        logger.info("Got " + games.size() + " games from ESPN API");
        return games;
    }
}
