package com.tallyo.tallyo_backend.model.espn.scoreboard;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Weather {
    private String displayValue;
    private int temperature;
    private int highTemperature;
    private String conditionId;
}
