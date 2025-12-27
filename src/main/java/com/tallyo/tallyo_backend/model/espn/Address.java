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

    @Override
    public String toString() {
        if(city != null && state != null && country != null && !country.equals("USA")){
            return String.format("%s, %s, %s", city, state, country);
        } else if (state == null) {
            return String.format("%s, %s", city, country);
        }else{
            return String.format("%s, %s", city, state);
        }
    }
}
