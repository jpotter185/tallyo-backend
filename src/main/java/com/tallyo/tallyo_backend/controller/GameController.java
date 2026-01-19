package com.tallyo.tallyo_backend.controller;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.dto.PageResponse;
import com.tallyo.tallyo_backend.dto.UpdateResponse;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.service.CalendarService;
import com.tallyo.tallyo_backend.service.GameServiceImpl;
import com.tallyo.tallyo_backend.service.CalendarServiceImpl;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    private final GameServiceImpl gameServiceImpl;
    private final CalendarService calendarService;
    public GameController(GameServiceImpl gameServiceImpl, CalendarServiceImpl calendarService){
        this.gameServiceImpl = gameServiceImpl;
        this.calendarService = calendarService;
    }

    private League getLeagueEnumFromString(String league) throws BadRequestException {
        League leagueEnum;
        try {
            leagueEnum = League.valueOf(league.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid league");
        }
        return leagueEnum;
    }

    @GetMapping("/context")
    public CurrentContext getCurrentContext(@RequestParam String league) throws BadRequestException {
        League leagueEnum = getLeagueEnumFromString(league);
        return calendarService.getCurrentContext(leagueEnum);

    }

    @GetMapping
    public PageResponse<Game> getGames(
            @RequestParam String league,
            @RequestParam(defaultValue = "0") Integer year,
            @RequestParam(defaultValue = "0") Integer seasonType,
            @RequestParam(defaultValue = "0") Integer week,
            @RequestParam(defaultValue = "100") Integer size,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "id") String sortBy
    ) throws BadRequestException {
        logger.info("Started getGames with params: league:{}, year:{},seasonType:{},week:{}, size:{}, page:{},sortBy:{}",
                league,
                year,
                seasonType,
                week,
                size,
                page,
                sortBy);

        long startTime = System.currentTimeMillis();
        League leagueEnum = getLeagueEnumFromString(league);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        Page<Game> pages = gameServiceImpl.getGames(leagueEnum, year, seasonType, week, pageable);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        logger.info("getGames took " + duration + "ms, got " + pages.getTotalElements() + " games");

        return new PageResponse<>(pages);

    }

    @GetMapping("/current")
    public PageResponse<Game> getCurrentWeekGames(
            @RequestParam String league,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer seasonType,
            @RequestParam(required = false) Integer week,
            @RequestParam(defaultValue = "100") Integer size,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "id") String sortBy
    ) throws BadRequestException {
        logger.info("Started getCurrentGames with params: league:{}, year:{},seasonType:{},week:{}, size:{}, page:{},sortBy:{}",
                league,
                year,
                seasonType,
                week,
                size,
                page,
                sortBy);

        long startTime = System.currentTimeMillis();
        League leagueEnum = getLeagueEnumFromString(league);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        CurrentContext context = calendarService.getCurrentContext(leagueEnum);
        int actualYear = calendarService.getCurrentYear();
        int actualSeasonType = context.seasonType();
        int actualWeek = context.week();
        Page<Game> pages =  gameServiceImpl.getGames(
                leagueEnum,
                actualYear,
                actualSeasonType,
                actualWeek,
                pageable
        );
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        logger.info("getGames took " + duration + "ms, got " + pages.getTotalElements() + " games");
        return new PageResponse<>(pages);
    }

    @PostMapping
    public UpdateResponse updateGames(@RequestParam String league,
                                      @RequestParam(defaultValue = "0") int year,
                                      @RequestParam(defaultValue = "false") boolean shouldFetchStats) throws BadRequestException {

        long startTime = System.currentTimeMillis();

        League leagueEnum = getLeagueEnumFromString(league);
        List<Game> games =  gameServiceImpl.updateGames(leagueEnum, year, shouldFetchStats);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        logger.info("updateGames took " + duration + "ms, got " + games.size() + " games");
        return new UpdateResponse(games.size(), duration);
    }

}
