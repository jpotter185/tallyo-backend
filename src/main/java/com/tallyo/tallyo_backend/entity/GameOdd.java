package com.tallyo.tallyo_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "game_odds")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameOdd {
    @Id
    private Integer id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "game_id", nullable = false)
    @JsonIgnore
    private Game game;

    private String spreadText;



}