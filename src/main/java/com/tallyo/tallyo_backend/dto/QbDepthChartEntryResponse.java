package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QbDepthChartEntryResponse {
    private Integer rank;
    private String athleteId;
    private String name;
    // The player's current injury designation, or null if not on the injury report.
    private String injuryStatus;
}
