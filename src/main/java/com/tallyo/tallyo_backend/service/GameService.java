package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.dto.GameDetailsResponse;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GameService {
    Page<Game> getGames(League league, int year, int seasonType, int week, String date, String timezone, Pageable pageable);

    /** Games for the league's current week (season leagues) or day (date leagues). */
    Page<Game> getCurrentGames(League league, CurrentContext context, String timezone, Pageable pageable);

    List<Game> updateGames(League league, int year, boolean shouldFetchStats);

    GameDetailsResponse getGameDetails(int gameId);
}
