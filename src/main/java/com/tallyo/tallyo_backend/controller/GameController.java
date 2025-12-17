package com.tallyo.tallyo_backend.controller;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.service.GameServiceImpl;
import org.apache.coyote.BadRequestException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameServiceImpl gameServiceImpl;
    public GameController(GameServiceImpl gameServiceImpl){
        this.gameServiceImpl = gameServiceImpl;
    }

    @GetMapping
    public List<Game> getGames(@RequestParam String league){
        return gameServiceImpl.getGames(league);
    }

    @PostMapping()
    public String updatedGames(@RequestParam String league) throws BadRequestException {
        if(!league.toUpperCase().equals(String.valueOf(League.CFB)) && !league.toUpperCase().equals(String.valueOf(League.NFL))){
            throw new BadRequestException("Invalid league");
        }
        return "Updated " + gameServiceImpl.updateGames(league) + " games";
    }

}
