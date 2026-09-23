package com.tallyo.tallyo_backend.entity;

import com.tallyo.tallyo_backend.enums.League;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TeamInjuryKey implements Serializable {
    @Enumerated(EnumType.STRING)
    private League league;
    private Integer teamId;
    private String athleteId;
}
