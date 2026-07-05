package com.tallyo.tallyo_backend.model.espn.box_score;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Statistic {
    String name;
    String displayValue;
    Object value;
    String label;
    // Baseball summaries group team stats (batting/pitching/fielding) with the
    // real stats nested under "stats"; other sports leave this null.
    List<Statistic> stats;
}