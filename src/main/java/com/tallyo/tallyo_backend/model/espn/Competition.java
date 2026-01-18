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
public class Competition {
    private String id;
    private String uid;
    private String date;
    private int attendance;
    private CompetitionType type;
    private boolean timeValid;
    private boolean neutralSite;
    private boolean conferenceCompetition;
    private boolean playByPlayAvailable;
    private boolean recent;
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

