package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class GameServiceImpl implements GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

    private final GameRepository gameRepository;
    private final EspnService espnService;

    public GameServiceImpl(GameRepository gameRepository, EspnService espnService) {
        this.gameRepository = gameRepository;
        this.espnService = espnService;
    }

    @Override
    public Page<Game> getGames(League league, int year, int seasonType, int week, String date, String timezone, Pageable pageable) {

        LocalDate localDate = null;
        Instant utcStart = null;
        Instant utcEnd = null;
        if (!date.isEmpty() && !timezone.isEmpty()) {
            localDate = LocalDate.parse(date);
            ZoneId userZone = ZoneId.of(timezone);
            ZonedDateTime startOfDay = localDate.atStartOfDay(userZone);
            ZonedDateTime endOfDay = localDate.plusDays(1).atStartOfDay(userZone);
            utcStart = startOfDay.toInstant();
            utcEnd = endOfDay.toInstant();
        }
        return gameRepository.getGames(league, year, seasonType, week, utcStart, utcEnd, pageable);
    }

    @Override
    public List<Game> updateGames(League league, int year, boolean shouldFetchStats) {
        List<Game> games = espnService.fetchGames(league, year, shouldFetchStats);
        gameRepository.saveAll(games);
        return games;
    }

    @Scheduled(fixedRate = 20000)
    public void updateGamesForToday() {
        Instant startDate = Instant.now();
        logger.info("Scheduled- Updating games for today");
        try {
            if (!gameRepository.shouldUpdate()) {
                return;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String yesterday = LocalDate.now().minusDays(1).format(formatter);
            String today = LocalDate.now().format(formatter);
            List<Game> nflGames = espnService.fetchGames(League.NFL, yesterday, today, true);
            gameRepository.saveAll(nflGames);
            List<Game> cfbGames = espnService.fetchGames(League.CFB, yesterday, today, true);
            gameRepository.saveAll(cfbGames);
            List<Game> nhlGames = espnService.fetchGames(League.NHL, yesterday, today, true);
            gameRepository.saveAll(nhlGames);
            logger.info("Finished updating games for today, updated {} games in {}ms ",
                    nflGames.size() + cfbGames.size() + nhlGames.size(),
                    Instant.now().toEpochMilli() - startDate.toEpochMilli());
        } catch (Exception e) {
            logger.error("Error updating games:");
            logger.error(e.getMessage());
        }
    }
}
