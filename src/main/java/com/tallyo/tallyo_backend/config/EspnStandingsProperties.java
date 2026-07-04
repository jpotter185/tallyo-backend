package com.tallyo.tallyo_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "espn.standings")
public class EspnStandingsProperties {

    /**
     * League id (lowercase enum name) -> ordered list of ESPN standings URLs.
     * URLs are tried in order until one returns a payload that maps to a
     * non-empty standings result.
     */
    private Map<String, List<String>> urls = Map.of();

    public List<String> getUrlsForLeague(String leagueId) {
        return new ArrayList<>(urls.getOrDefault(leagueId, List.of()));
    }
}
