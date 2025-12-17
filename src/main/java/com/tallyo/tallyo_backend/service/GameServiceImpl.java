package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
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
    public List<Game> getGames(String league) {
        return gameRepository.findAll();
    }

    @Override
    public int updateGames(String league) {
        List<Game> games = espnService.fetchGames(league);
        return(gameRepository.saveAll(games).size());
    }
}
