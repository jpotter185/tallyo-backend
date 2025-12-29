package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.repository.GameRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CalendarServiceImpl implements CalendarService{

    private final GameRepository gameRepository;

    public CalendarServiceImpl(GameRepository gameRepository){
        this.gameRepository = gameRepository;
    }


    @Override
    public int getCurrentYear() {
        LocalDate now = LocalDate.now();
        return now.getMonthValue() >= 9 ? now.getYear() : now.getYear() - 1;
    }

    @Override
    @Cacheable(value = "currentContext", key = "#league")
    public CurrentContext getCurrentContext(League league) {
        CurrentContext context = gameRepository.findCurrentContext(league);
        return context != null ? context : new CurrentContext(getCurrentYear(), 2, 1);
    }
}
