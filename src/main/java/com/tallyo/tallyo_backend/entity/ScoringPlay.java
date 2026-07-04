package com.tallyo.tallyo_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scoring_plays", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_id", "play_id"})
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScoringPlay {
    @EmbeddedId
    private ScoringPlayKey key;

    @ManyToOne
    @MapsId("gameId")
    @JoinColumn(name = "game_id", nullable = false)
    @JsonBackReference
    private Game game;

    private String teamId;
    private String teamAbbreviation;
    @Column(length = 1024)
    private String displayText;
    private Integer homeScore;
    private Integer awayScore;
    private String scoringType;
    private Integer period;
    private String clock;
    private Integer sequence;
}
