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
public class Competition {
    private String id;
    private String uid;
    private String date;
    private Integer attendance;
    private CompetitionType type;
    private Boolean timeValid;
    private Boolean neutralSite;
    private Boolean conferenceCompetition;
    private Boolean playByPlayAvailable;
    private Boolean recent;
    private Venue venue;
    private List<Competitor> competitors;
    private List<Note> notes;
    private Situation situation;
    private Status status;
    private GameFormat format;
    private String startDate;
    private String broadcast;
    private List<Object> highlights;
    private List<Odds> odds;
}

