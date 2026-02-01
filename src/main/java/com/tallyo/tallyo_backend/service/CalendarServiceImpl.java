package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

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
    public CurrentContext getCurrentContext(League league, String timezone) {
        logger.info("Getting current context for league:{}", league);

        CurrentContext context = gameRepository.findCurrentContext(league);
        LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));
        String formatted = today.toString();

        CurrentContext retContext = context != null ?
                context :
                new CurrentContext(getCurrentYear(), 2, formatted, 1);
        retContext.setDate(formatted);
        logger.info("Got current context for league:{}, year:{}, seasonType:{}, week:{}, date:{}",
                league.getValue(),
                retContext.getYear(),
                retContext.getSeasonType(),
                retContext.getWeek(),
                retContext.getDate());
        return retContext;
    }

    @Override
    public List<String> getNhlGameDates(String timezone) {
        ZoneId userZone = ZoneId.of(timezone);
        List<Instant> gameTimes = gameRepository.getNhlGameDates();

        return gameTimes.stream()
                .map(instant -> instant.atZone(userZone).toLocalDate())
                .distinct() // Remove duplicates
                .sorted()
                .map(LocalDate::toString)
                .collect(Collectors.toList());
    }
}
