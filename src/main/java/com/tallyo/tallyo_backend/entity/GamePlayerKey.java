package com.tallyo.tallyo_backend.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayerKey implements Serializable {
    private Integer gameId;
    private Integer teamId;
    private String playerId;
    // ESPN stat group type, e.g. "batting"/"pitching"; a two-way player can
    // appear once per group.
    private String category;
}
