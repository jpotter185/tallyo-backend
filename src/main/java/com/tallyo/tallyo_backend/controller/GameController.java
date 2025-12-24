package com.tallyo.tallyo_backend.controller;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.model.GameResponse;
import com.tallyo.tallyo_backend.service.GameServiceImpl;
import org.apache.coyote.BadRequestException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameServiceImpl gameServiceImpl;
    public GameController(GameServiceImpl gameServiceImpl){
        this.gameServiceImpl = gameServiceImpl;
    }

    @GetMapping
    public GameResponse getGames(
            @RequestParam String league,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int seasonType,
            @RequestParam(defaultValue = "0") int week
    ) throws BadRequestException {

        long startTime = System.currentTimeMillis();
        League leagueEnum;
        try {
            leagueEnum = League.valueOf(league.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid league");
        }

        List<Game> games =  gameServiceImpl.getGames(
                leagueEnum,
                year,
                seasonType,
                week
        );
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("getGames took " + duration + "ms, got " + games.size() + " games");
        return new GameResponse(games, games.size());
    }

    @PostMapping()
    public GameResponse updateGames(@RequestParam String league,
                                  @RequestParam(defaultValue = "0") int year) throws BadRequestException {

        long startTime = System.currentTimeMillis();
        League leagueEnum;
        try {
            leagueEnum = League.valueOf(league.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid league");
        }
        List<Game> games =  gameServiceImpl.updateGames(leagueEnum, year);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("updateGames took " + duration + "ms, got " + games.size() + " games");
        return new GameResponse(games, games.size());
    }

}
