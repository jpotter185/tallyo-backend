package com.tallyo.tallyo_backend.dto;

import com.tallyo.tallyo_backend.enums.League;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LeagueMetadataResponse {
    private String id;
    private String label;
    private String path;
    private boolean supportsStandings;
    private String contextMode;
    private boolean supportsYearFilter;
    private boolean supportsWeekFilter;
    private String statsProfile;
    private boolean showInHeader;
    private boolean showInDashboard;

    public static LeagueMetadataResponse fromLeague(League league) {
        return LeagueMetadataResponse.builder()
                .id(league.getId())
                .label(league.getLabel())
                .path(league.getPath())
                .supportsStandings(league.isSupportsStandings())
                .contextMode(league.getContextMode())
                .supportsYearFilter(league.isSupportsYearFilter())
                .supportsWeekFilter(league.isSupportsWeekFilter())
                .statsProfile(league.getStatsProfile())
                .showInHeader(true)
                .showInDashboard(true)
                .build();
    }
}
