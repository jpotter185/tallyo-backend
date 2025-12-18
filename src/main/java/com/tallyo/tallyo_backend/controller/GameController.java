package com.tallyo.tallyo_backend.controller;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
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
    public List<Game> getGames(@RequestParam String league) throws BadRequestException {
        boolean isCfb = league.toUpperCase().equals(League.CFB.toString());
        boolean isNfl = league.toUpperCase().equals(League.NFL.toString());
        if(!isNfl && !isCfb){
            throw new BadRequestException("Invalid league");
        }


        return gameServiceImpl.getGames(isCfb ? League.CFB: League.NFL);
    }

    @PostMapping()
    public String updateGames(@RequestParam String league) throws BadRequestException {
        boolean isCfb = league.toUpperCase().equals(League.CFB.toString());
        boolean isNfl = league.toUpperCase().equals(League.NFL.toString());
        if(!isNfl && !isCfb){
            throw new BadRequestException("Invalid league");
        }
        return "Updated " +
                gameServiceImpl.updateGames(isCfb ?
                        League.CFB :
                        League.NFL)
                + " games";
    }

}
