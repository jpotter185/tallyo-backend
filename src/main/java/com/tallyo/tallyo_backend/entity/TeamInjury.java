package com.tallyo.tallyo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

// A player's *current* injury designation, from ESPN's team roster. Rows for a team
// are replaced wholesale on every refresh, so recovered players drop off; there is
// no history (ESPN only exposes the current state).
@Entity
@Table(name = "team_injuries")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamInjury {
    @EmbeddedId
    private TeamInjuryKey key;

    private String teamAbbreviation;
    private String athleteName;
    private String position;
    // ESPN designation, e.g. "Questionable", "Doubtful", "Out", "Injured Reserve".
    private String status;
    // When ESPN last changed this designation.
    private Instant statusDate;
    // When tallyo fetched it.
    private Instant updatedAt;
}
