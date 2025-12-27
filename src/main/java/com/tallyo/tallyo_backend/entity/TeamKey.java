package com.tallyo.tallyo_backend.entity;

import com.tallyo.tallyo_backend.enums.League;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamKey implements Serializable {

    private int teamId;
    @Enumerated(EnumType.STRING)
    private League league;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamKey teamId1 = (TeamKey) o;
        return teamId == teamId1.teamId && Objects.equals(league, teamId1.league);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, league);
    }
}
