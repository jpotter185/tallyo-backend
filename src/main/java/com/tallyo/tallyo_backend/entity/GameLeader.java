package com.tallyo.tallyo_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "game_leaders", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_id", "stat_name", "team_id"})
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameLeader {
    @EmbeddedId
    private GameLeaderKey key;

    @ManyToOne
    @MapsId("gameId")
    @JoinColumn(name = "game_id", nullable = false)
    @JsonBackReference
    private Game game;

    private String displayName;
    private Double statValue;
    private String displayValue;
    private String playerName;
    private String playerShortName;
}
