package com.tallyo.tallyo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

// A team's current quarterback depth chart from ESPN. ESPN's depth chart does not
// reflect injuries (a Doubtful starter stays at rank 1), so consumers combine it
// with TeamInjury to project who actually starts. Replaced wholesale on refresh.
@Entity
@Table(name = "team_qb_depth_charts")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QbDepthChartEntry {
    @EmbeddedId
    private QbDepthChartKey key;

    private String teamAbbreviation;
    private String athleteId;
    private String athleteName;
    private Instant updatedAt;
}
