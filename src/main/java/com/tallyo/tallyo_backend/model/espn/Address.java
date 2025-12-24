package com.tallyo.tallyo_backend.model.espn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Address {
    private String city;
    private String state;
    private String country;
}
