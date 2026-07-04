package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.dto.StandingsGroupResponse;
import com.tallyo.tallyo_backend.enums.League;

import java.util.List;

public interface StandingsService {
    List<StandingsGroupResponse> getStandings(League league);
}
