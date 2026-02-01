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
        logger.info("Getting games for league:{}, year:{}, seasonType:{}, week:{}, date{}",
                league.getValue(),
                year,
                seasonType,
                week,
                date);
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
        logger.info("localDate:{}, userTimezone:{}, Start of day: {}, end of day:{}", localDate, timezone, utcStart, utcEnd);
        return gameRepository.getGames(league, year, seasonType, week, utcStart, utcEnd, pageable);
    }

    @Override
    public List<Game> updateGames(League league, int year, boolean shouldFetchStats) {
        List<Game> games = espnService.fetchGames(league, year, shouldFetchStats);
        gameRepository.saveAll(games);
        logger.info("Got " + games.size() + " games from ESPN API");
        return games;
    }

    @Scheduled(fixedRate = 10000)
    public void updateGamesForToday() {
        logger.info("Updating games for today");
        try {
            logger.info("Checking if we should update");
            if (!gameRepository.shouldUpdate()) {
                logger.info("No games currently in progress");
                return;
            }
            logger.info("we should update");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String yesterday = LocalDate.now().minusDays(1).format(formatter);
            String today = LocalDate.now().format(formatter);
            List<Game> nflGames = espnService.fetchGames(League.NFL, yesterday, today, true);
            logger.info("Got " + nflGames.size() + " nfl games from ESPN API");
            gameRepository.saveAll(nflGames);
            List<Game> cfbGames = espnService.fetchGames(League.CFB, yesterday, today, true);
            logger.info("Got " + cfbGames.size() + " cfb games from ESPN API");
            gameRepository.saveAll(cfbGames);
            List<Game> nhlGames = espnService.fetchGames(League.NHL, yesterday, today, true);
            logger.info("Got " + nhlGames.size() + " nhl games from ESPN API");
            gameRepository.saveAll(nhlGames);
            logger.info("Finished updating games");
        } catch (Exception e) {
            logger.error("Error updating games:");
            logger.error(e.getMessage());
        }
    }
}
