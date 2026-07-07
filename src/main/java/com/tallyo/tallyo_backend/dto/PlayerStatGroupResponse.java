package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PlayerStatGroupResponse {
    private Integer teamId;
    private String category;
    private List<String> labels;
    private List<PlayerStatLineResponse> players;
}
