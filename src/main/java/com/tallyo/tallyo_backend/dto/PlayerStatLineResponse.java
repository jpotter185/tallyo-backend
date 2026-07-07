package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PlayerStatLineResponse {
    private String playerId;
    private String playerName;
    private String playerShortName;
    private String position;
    private Integer batOrder;
    private Boolean starter;
    // Values aligned index-for-index with the group's labels array.
    private List<String> stats;
}
