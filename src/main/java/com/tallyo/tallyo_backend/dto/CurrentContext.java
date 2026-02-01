package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CurrentContext {
    private int year;
    private int seasonType;
    private String date;
    private int week;
}
