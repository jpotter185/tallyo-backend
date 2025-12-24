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
public class Event {
    private String id;
    private String uid;
    private String date;
    private String name;
    private String shortName;
    private Season season;
    private Week week;
    private List<Competition> competitions;
    private Weather weather;
    private List<Link> links;
    private Status status;
}

