package com.tallyo.tallyo_backend.model.espn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Competitor {
    private int id;
    private String uid;
    private String type;
    private int order;
    private String homeAway;
    private Team team;
    private String score;
    private List<Object> statistics;
    private List<Record> records;
    private Boolean winner;
}
