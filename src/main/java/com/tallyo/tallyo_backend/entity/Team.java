package com.tallyo.tallyo_backend.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "teams")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Team {
    @EmbeddedId
    private TeamKey teamKey;

    private String name;
    private String abbreviation;
    private String logo;
    private String primaryColor;
    private String alternateColor;
    private String location;
    private String record;
    private String seed;
    private String ranking;
    private String wins;
    private String losses;
    private String ties;
    private String conference;
    private String division;
    private String homeRecord;
    private String roadRecord;
    private String recordVsConference;
    private String recordVsDivision;
    private String pointsFor;
    private String pointsAgainst;
    private String pointDifferential;
    private String streak;
    private String winPercent;
    private String additionalJsonFields;
    private String score;
}
