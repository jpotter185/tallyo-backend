package com.tallyo.tallyo_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "game_players", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_id", "team_id", "player_id", "category"})
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GamePlayer {
    @EmbeddedId
    private GamePlayerKey key;

    @ManyToOne
    @MapsId("gameId")
    @JoinColumn(name = "game_id", nullable = false)
    @JsonBackReference
    private Game game;

    private String playerName;
    private String playerShortName;
    private String position;
    private Integer batOrder;
    private Boolean starter;
    // Position within ESPN's table for this team+category; preserves lineup order.
    private Integer displayOrder;
    // Pipe-joined column labels/values exactly as ESPN sends them (game stats for
    // live/final games, season stats for scheduled lineups); values never contain pipes.
    private String statLabels;
    private String statValues;
}
