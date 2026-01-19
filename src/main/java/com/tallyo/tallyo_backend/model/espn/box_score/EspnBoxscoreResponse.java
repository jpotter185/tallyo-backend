package com.tallyo.tallyo_backend.model.espn.box_score;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class EspnBoxscoreResponse {
    Boxscore boxscore;
}
