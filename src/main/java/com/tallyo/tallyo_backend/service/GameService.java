package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;

import java.util.List;

public interface GameService {
    List<Game> getGames(League league);
    int updateGames(League league);
}
