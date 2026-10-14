package com.tallyo.tallyo_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "game_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_id", "stat_type", "team_id"})
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameStat {
    @EmbeddedId
    private GameStatKey key;

    @ManyToOne
    @MapsId("gameId")
    @JoinColumn(name = "game_id", nullable = false)
    @JsonBackReference
    private Game game;

    @Column(nullable = false, columnDefinition = "text")
    private String statValue;

}