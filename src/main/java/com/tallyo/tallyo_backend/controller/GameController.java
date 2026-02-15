package com.tallyo.tallyo_backend.controller;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.dto.GameResponse;
import com.tallyo.tallyo_backend.dto.PageResponse;
import com.tallyo.tallyo_backend.dto.UpdateResponse;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.exception.InvalidRequestException;
import com.tallyo.tallyo_backend.mapper.GameResponseMapper;
import com.tallyo.tallyo_backend.service.CalendarService;
import com.tallyo.tallyo_backend.service.GameServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameServiceImpl gameServiceImpl;
    private final CalendarService calendarService;
    private final GameResponseMapper gameResponseMapper;

    public GameController(GameServiceImpl gameServiceImpl,
                          CalendarService calendarService,
                          GameResponseMapper gameResponseMapper) {
        this.gameServiceImpl = gameServiceImpl;
        this.calendarService = calendarService;
        this.gameResponseMapper = gameResponseMapper;
    }

    private League getLeagueEnumFromString(String league) {
        try {
            return League.valueOf(league.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid league");
        }
    }

    @GetMapping("/nhl-dates")
    public List<String> getNhlDate(
            @RequestParam(defaultValue = "America/New_York") String userTimeZone) {
        return calendarService.getGameDates(League.NHL, userTimeZone);
    }

    @GetMapping("/dates")
    public List<String> getLeagueDates(
            @RequestParam String league,
            @RequestParam(defaultValue = "America/New_York") String userTimeZone) {
        League leagueEnum = getLeagueEnumFromString(league);
        return calendarService.getGameDates(leagueEnum, userTimeZone);
    }

    @GetMapping("/context")
    public CurrentContext getCurrentContext(
            @RequestParam String league,
            @RequestParam(defaultValue = "America/New_York") String userTimeZone) {
        League leagueEnum = getLeagueEnumFromString(league);

        return calendarService.getCurrentContext(leagueEnum, userTimeZone);
    }

    @GetMapping
    public PageResponse<GameResponse> getGames(
            @RequestParam String league,
            @RequestParam(defaultValue = "0") Integer year,
            @RequestParam(defaultValue = "0") Integer seasonType,
            @RequestParam(defaultValue = "0") Integer week,
            @RequestParam(defaultValue = "100") Integer size,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "America/New_York") String userTimeZone
    ) {
        League leagueEnum = getLeagueEnumFromString(league);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<Game> pages = gameServiceImpl.getGames(leagueEnum, year, seasonType, week, date, userTimeZone, pageable);
        return new PageResponse<>(pages.map(gameResponseMapper::toResponse));

    }

    @GetMapping("/current")
    public PageResponse<GameResponse> getCurrentWeekGames(
            @RequestParam String league,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer seasonType,
            @RequestParam(required = false) Integer week,
            @RequestParam(defaultValue = "America/New_York") String userTimeZone,
            @RequestParam(defaultValue = "100") Integer size,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "id") String sortBy
    ) {

        League leagueEnum = getLeagueEnumFromString(league);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        CurrentContext context = calendarService.getCurrentContext(leagueEnum, userTimeZone);
        int actualYear = calendarService.getCurrentYear();
        int actualSeasonType = context.getSeasonType();
        Page<Game> pages = gameServiceImpl.getGames(
                leagueEnum,
                leagueEnum.isSupportsYearFilter() ? actualYear : 0,
                actualSeasonType,
                leagueEnum.isSupportsWeekFilter() ? context.getWeek() : 0,
                context.getDate(),
                userTimeZone,
                pageable
        );
        return new PageResponse<>(pages.map(gameResponseMapper::toResponse));
    }

    @PostMapping
    public UpdateResponse updateGames(@RequestParam String league,
                                      @RequestParam(defaultValue = "0") int year,
                                      @RequestParam(defaultValue = "false") boolean shouldFetchStats) {

        long startTime = System.currentTimeMillis();
        League leagueEnum = getLeagueEnumFromString(league);
        List<Game> games = gameServiceImpl.updateGames(leagueEnum, year, shouldFetchStats);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        return new UpdateResponse(games.size(), duration);
    }

}
