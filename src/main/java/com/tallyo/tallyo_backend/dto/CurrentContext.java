package com.tallyo.tallyo_backend.dto;

import java.time.Instant;

public record CurrentContext(int year, int seasonType, Instant date, int week) {
}
