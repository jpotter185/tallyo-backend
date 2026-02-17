package com.tallyo.tallyo_backend.model.espn.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Weather {
    private String displayValue;
    private Integer temperature;
    private Integer highTemperature;
    private String conditionId;
}
