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
public class ScoringPlayKey implements Serializable {
    private Integer gameId;
    private String playId;
}
