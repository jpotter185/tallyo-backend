package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GameDetailsResponse {
    private String gameId;
    private List<StatLeaderResponse> leaders;
    private List<ScoringPlayResponse> scoringPlays;
    private List<PlayerStatGroupResponse> players;
}
