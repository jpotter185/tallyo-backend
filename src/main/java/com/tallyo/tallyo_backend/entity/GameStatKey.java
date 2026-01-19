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
public class GameStatKey implements Serializable {
    private Integer gameId;
    private Integer teamId;
    private String statType;
}
