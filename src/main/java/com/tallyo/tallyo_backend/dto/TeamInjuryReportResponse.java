package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class TeamInjuryReportResponse {
    private Integer teamId;
    private String abbreviation;
    private String updatedAt;
    private List<InjuredPlayerResponse> injuries;
    private List<QbDepthChartEntryResponse> qbDepthChart;
}
