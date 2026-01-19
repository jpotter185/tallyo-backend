package com.tallyo.tallyo_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatResponse {
    List<StatObject> homeStats;
    List<StatObject> awayStats;
}
