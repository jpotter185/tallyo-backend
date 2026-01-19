package com.tallyo.tallyo_backend.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatResponse {
    Map<String, String> homeStats;
    Map<String, String> awayStats;
}
