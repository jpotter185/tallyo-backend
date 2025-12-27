package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UpdateResponse {
    private final int gameCount;
    private final Long durationMs;
}
