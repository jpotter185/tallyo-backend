package com.tallyo.tallyo_backend.model.espn.box_score;

import lombok.*;

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
}