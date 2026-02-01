package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.enums.League;

import java.util.List;

public interface CalendarService {
    CurrentContext getCurrentContext(League league, String timezone);

    int getCurrentYear();

    List<String> getNhlGameDates(String timezone);
}
