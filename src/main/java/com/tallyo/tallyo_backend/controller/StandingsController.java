package com.tallyo.tallyo_backend.controller;

import com.tallyo.tallyo_backend.dto.StandingsGroupResponse;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.exception.InvalidRequestException;
import com.tallyo.tallyo_backend.service.StandingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/standings")
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @GetMapping
    public List<StandingsGroupResponse> getStandings(@RequestParam String league) {
        League leagueEnum;
        try {
            leagueEnum = League.valueOf(league.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid league");
        }
        return standingsService.getStandings(leagueEnum);
    }
}
