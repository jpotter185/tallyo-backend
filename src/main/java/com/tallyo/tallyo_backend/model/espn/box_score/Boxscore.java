package com.tallyo.tallyo_backend.model.espn.box_score;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Boxscore {
    List<Team> teams;

}
