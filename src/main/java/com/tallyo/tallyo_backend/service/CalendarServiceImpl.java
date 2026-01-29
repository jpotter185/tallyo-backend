package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CalendarServiceImpl implements CalendarService {

    private final GameRepository gameRepository;
    private static final Logger logger = LoggerFactory.getLogger(CalendarServiceImpl.class);

    public CalendarServiceImpl(GameRepository gameRepository) {
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
        logger.info("Getting current context for league:{}", league);
        CurrentContext context = gameRepository.findCurrentContext(league);
        CurrentContext retContext = context != null ? context : new CurrentContext(getCurrentYear(), 2, LocalDate.now().toString(), 1);
        logger.info("Got current context for league:{}, year:{}, seasonType:{}, week:{}",
                league.getValue(),
                retContext.year(),
                retContext.seasonType(),
                retContext.week());
        return retContext;
    }

    @Override
    public List<String> getNhlGameDates() {
        return gameRepository.getNhlGameDates();
    }
}
