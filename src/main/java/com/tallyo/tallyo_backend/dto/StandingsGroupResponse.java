package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class StandingsGroupResponse {
    private String groupName;
    private List<StandingsTeamResponse> teams;
}
