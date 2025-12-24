package com.tallyo.tallyo_backend.model.espn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@Builder
public class EspnScoreboardResponse {
    private List<League> leagues;
    private Season season;
    private Week week;
    private List<Event> events;
}

