package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;

import java.util.List;

public interface GameService {
    List<Game> getGames(String league);
    int updateGames(String league);
}
