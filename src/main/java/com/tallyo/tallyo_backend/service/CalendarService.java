package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.enums.League;

public interface CalendarService {
    CurrentContext getCurrentContext(League league);
    int getCurrentYear();
}
