package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InjuredPlayerResponse {
    private String athleteId;
    private String name;
    private String position;
    private String status;
    private String statusDate;
}
