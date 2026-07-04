package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StatLeaderResponse {
    private String name;
    private String displayName;
    private Double value;
    private String displayValue;
    private String playerName;
    private String playerShortName;
    private Integer teamId;
}
