package com.tallyo.tallyo_backend.service;

import tools.jackson.databind.JsonNode;
import com.tallyo.tallyo_backend.config.EspnStandingsProperties;
import com.tallyo.tallyo_backend.dto.StandingsGroupResponse;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.exception.InvalidRequestException;
import com.tallyo.tallyo_backend.mapper.EspnStandingsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class StandingsServiceImpl implements StandingsService {

    private static final Logger log = LoggerFactory.getLogger(StandingsServiceImpl.class);

    private final RestTemplate restTemplate;
    private final EspnStandingsProperties standingsProperties;
    private final EspnStandingsMapper standingsMapper;

    public StandingsServiceImpl(RestTemplate restTemplate,
                                EspnStandingsProperties standingsProperties,
                                EspnStandingsMapper standingsMapper) {
        this.restTemplate = restTemplate;
        this.standingsProperties = standingsProperties;
        this.standingsMapper = standingsMapper;
    }

    @Override
    @Cacheable(value = "standings", key = "#league.name()", unless = "#result.isEmpty()")
    public List<StandingsGroupResponse> getStandings(League league) {
        if (!league.isSupportsStandings()) {
            throw new InvalidRequestException("Standings not supported for league: " + league.getId());
        }
        List<String> urls = standingsProperties.getUrlsForLeague(league.getId());
        if (urls.isEmpty()) {
            throw new InvalidRequestException("No standings source configured for league: " + league.getId());
        }

        RestClientException lastError = null;
        for (String url : urls) {
            try {
                JsonNode payload = restTemplate.getForObject(url, JsonNode.class);
                List<StandingsGroupResponse> standings = standingsMapper.map(payload);
                if (!standings.isEmpty()) {
                    return standings;
                }
                log.warn("Standings URL returned no mappable groups for {}: {}", league, url);
            } catch (RestClientException e) {
                log.warn("Standings fetch failed for {} at {}: {}", league, url, e.getMessage());
                lastError = e;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        return List.of();
    }
}
