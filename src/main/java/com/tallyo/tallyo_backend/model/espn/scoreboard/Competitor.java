package com.tallyo.tallyo_backend.model.espn.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Competitor {
    private Integer id;
    private String uid;
    private String type;
    private Integer order;
    private String homeAway;
    private Team team;
    private String score;
    private List<Object> statistics;
    private List<Record> records;
    private Boolean winner;
}
