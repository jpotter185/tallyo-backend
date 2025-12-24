package com.tallyo.tallyo_backend.model.espn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class CompetitionType {
    private String id;
    private String abbreviation;
}

