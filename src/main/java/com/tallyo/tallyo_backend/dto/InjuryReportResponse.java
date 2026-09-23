package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class InjuryReportResponse {
    private String league;
    // Oldest per-team fetch time, so callers can tell how stale the whole report may be.
    private String updatedAt;
    private List<TeamInjuryReportResponse> teams;
}
