package com.tallyo.tallyo_backend.model.espn.box_score;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class EspnBoxscoreResponse {
    Boxscore boxscore;
    // Shapes of these vary per sport; parsed leniently by EspnSummaryDetailsMapper.
    JsonNode leaders;
    JsonNode scoringPlays;
    JsonNode plays;
    JsonNode keyEvents;
}
