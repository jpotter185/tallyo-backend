package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.dto.GameDetailsResponse;
import com.tallyo.tallyo_backend.dto.ScoringPlayResponse;
import com.tallyo.tallyo_backend.dto.StatLeaderResponse;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.ScoringPlay;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.exception.ResourceNotFoundException;
import com.tallyo.tallyo_backend.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GameServiceImpl implements GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);
    private static final Set<String> AWAITING_PLAY_STATUSES =
            Set.of("STATUS_SCHEDULED", "STATUS_POSTPONED");

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

    @Override
    @Transactional(readOnly = true)
    public GameDetailsResponse getGameDetails(int gameId) {
        Game game = gameRepository.findById((long) gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + gameId));

        List<StatLeaderResponse> leaders = game.getLeaders() == null
                ? List.of()
                : game.getLeaders().values().stream()
                .map(leader -> StatLeaderResponse.builder()
                        .name(leader.getKey().getStatName())
                        .displayName(leader.getDisplayName())
                        .value(leader.getStatValue())
                        .displayValue(leader.getDisplayValue())
                        .playerName(leader.getPlayerName())
                        .playerShortName(leader.getPlayerShortName())
                        .teamId(leader.getKey().getTeamId())
                        .build())
                .toList();

        List<ScoringPlayResponse> scoringPlays = game.getScoringPlays() == null
                ? List.of()
                : game.getScoringPlays().values().stream()
                .sorted(Comparator.comparing(ScoringPlay::getSequence,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(play -> ScoringPlayResponse.builder()
                        .id(play.getKey().getPlayId())
                        .teamId(play.getTeamId())
                        .teamName(play.getTeamAbbreviation())
                        .displayText(play.getDisplayText())
                        .homeScore(play.getHomeScore())
                        .awayScore(play.getAwayScore())
                        .scoringType(play.getScoringType())
                        .period(play.getPeriod())
                        .clock(play.getClock())
                        .build())
                .toList();

        return GameDetailsResponse.builder()
                .gameId(String.valueOf(gameId))
                .leaders(leaders)
                .scoringPlays(scoringPlays)
                .build();
    }

    @Scheduled(fixedRate = 20000)
    public void updateGamesForToday() {
        long startTime = System.currentTimeMillis();
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
            int updatedGameCount = 0;
            for (League league : League.values()) {
                List<Game> games = espnService.fetchGames(league, yesterday, today, true);
                gameRepository.saveAll(games);
                updatedGameCount += games.size();
            }
            logger.info("Finished updating games for today, updated {} games in {}ms ",
                    updatedGameCount,
                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logger.error("Error updating games:");
            logger.error(e.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshSchedulesOnStartup() {
        refreshSchedules();
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "America/New_York")
    public void refreshSchedulesDaily() {
        refreshSchedules();
    }

    // Seeds/refreshes the current year's schedule for every league so the 20s
    // live-update job has upcoming games to trigger on. Schedule-only (no
    // per-game summary calls): one scoreboard request per league.
    private void refreshSchedules() {
        long startTime = System.currentTimeMillis();
        logger.info("Refreshing season schedules for all leagues");
        for (League league : League.values()) {
            try {
                List<Game> games = espnService.fetchGames(league, 0, false);
                int saved = saveScheduleGames(games);
                logger.info("Refreshed schedule for {}: saved {} of {} games", league, saved, games.size());
            } catch (Exception e) {
                logger.error("Schedule refresh failed for {}: {}", league, e.getMessage());
            }
        }
        logger.info("Finished refreshing schedules in {}ms", System.currentTimeMillis() - startTime);
    }

    // A schedule-only Game carries null stats/leaders/scoringPlays collections;
    // merging one over an ingested game orphan-deletes those children. Only save
    // games that are new or still awaiting play (which have no details to lose) -
    // live games are owned by the 20s job and finished games don't change.
    private int saveScheduleGames(List<Game> games) {
        List<Long> ids = games.stream().map(game -> (long) game.getId()).toList();
        Map<Integer, String> existingStatuses = gameRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Game::getId,
                        game -> game.getGameStatus() == null ? "" : game.getGameStatus()));

        List<Game> toSave = games.stream()
                .filter(game -> {
                    String status = existingStatuses.get(game.getId());
                    return status == null || status.isEmpty() || AWAITING_PLAY_STATUSES.contains(status);
                })
                .toList();
        gameRepository.saveAll(toSave);
        return toSave.size();
    }
}
