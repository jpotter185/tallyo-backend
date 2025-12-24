package com.tallyo.tallyo_backend.model.espn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class StatusType {
    private String id;
    private String name;
    private String state;
    private boolean completed;
    private String description;
    private String detail;
    private String shortDetail;

    // getters & setters
}
