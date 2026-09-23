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
public class QbDepthChartKey implements Serializable {
    @Enumerated(EnumType.STRING)
    private League league;
    private Integer teamId;
    // 1 = listed starter.
    private Integer depthRank;
}
