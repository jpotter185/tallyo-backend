package com.tallyo.tallyo_backend.model.espn.box_score;

import lombok.*;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Boxscore {
    List<Team> teams;
    // Per-player stat tables; shape varies per sport, parsed leniently by
    // EspnSummaryDetailsMapper.
    JsonNode players;

}
