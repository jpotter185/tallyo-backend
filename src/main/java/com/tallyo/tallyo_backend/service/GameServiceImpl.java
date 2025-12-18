package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.repository.GameRepository;
import com.tallyo.tallyo_backend.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameServiceImpl implements GameService{

    private final GameRepository gameRepository;
    private final EspnService espnService;
    public GameServiceImpl(GameRepository gameRepository, TeamRepository teamRepository, EspnService espnService){
        this.gameRepository = gameRepository;
        this.espnService = espnService;
    }

    @Override
    public List<Game> getGames(League league) {
        return gameRepository.findByLeague(league);
    }

    @Override
    public int updateGames(League league) {
        List<Game> games = espnService.fetchGames(league);
        gameRepository.saveAll(games);
        return games.size();
    }
}
