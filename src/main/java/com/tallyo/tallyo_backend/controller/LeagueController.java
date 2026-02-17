package com.tallyo.tallyo_backend.controller;

import com.tallyo.tallyo_backend.dto.LeagueMetadataResponse;
import com.tallyo.tallyo_backend.enums.League;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/leagues")
public class LeagueController {

    @GetMapping
    public List<LeagueMetadataResponse> getLeagues() {
        return Arrays.stream(League.values())
                .map(LeagueMetadataResponse::fromLeague)
                .toList();
    }
}
