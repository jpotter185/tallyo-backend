package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.dto.GameDetailsResponse;
import com.tallyo.tallyo_backend.dto.PlayerStatGroupResponse;
import com.tallyo.tallyo_backend.dto.PlayerStatLineResponse;
import com.tallyo.tallyo_backend.dto.ScoringPlayResponse;
import com.tallyo.tallyo_backend.dto.StatLeaderResponse;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.GamePlayer;
import com.tallyo.tallyo_backend.entity.ScoringPlay;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.exception.InvalidRequestException;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private static final int SCHEDULE_LOOKBACK_DAYS = 7;

    private final GameRepository gameRepository;
    private final EspnService espnService;
    private final TransactionTemplate transactionTemplate;

    public GameServiceImpl(GameRepository gameRepository, EspnService espnService,
                           TransactionTemplate transactionTemplate) {
        this.gameRepository = gameRepository;
        this.espnService = espnService;
        this.transactionTemplate = transactionTemplate;
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
    public Page<Game> getCurrentGames(League league, CurrentContext context, String timezone, Pageable pageable) {
        String dateFilter = "date".equals(league.getContextMode()) ? context.getDate() : "";
        return getGames(
                league,
                league.isSupportsYearFilter() ? context.getYear() : 0,
                context.getSeasonType(),
                league.isSupportsWeekFilter() ? context.getWeek() : 0,
                dateFilter,
                timezone,
                pageable
        );
    }

    @Override
    public List<Game> updateGames(League league, int year, boolean shouldFetchStats) {
        if (league.isRetired()) {
            throw new InvalidRequestException("League " + league.getId() + " is retired and no longer updated");
        }
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
                .players(toPlayerStatGroups(game))
                .build();
    }

    // Regroups flat GamePlayer rows into one table per team+category, restoring
    // ESPN's lineup order and hoisting the (per-group identical) labels.
    private List<PlayerStatGroupResponse> toPlayerStatGroups(Game game) {
        if (game.getPlayers() == null || game.getPlayers().isEmpty()) {
            return List.of();
        }
        Map<Integer, Map<String, List<GamePlayer>>> grouped = game.getPlayers().values().stream()
                .collect(Collectors.groupingBy(player -> player.getKey().getTeamId(),
                        Collectors.groupingBy(player -> player.getKey().getCategory())));

        List<PlayerStatGroupResponse> result = new ArrayList<>();
        grouped.forEach((teamId, byCategory) -> byCategory.forEach((category, players) -> {
            List<GamePlayer> ordered = players.stream()
                    .sorted(Comparator.comparing(GamePlayer::getDisplayOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            result.add(PlayerStatGroupResponse.builder()
                    .teamId(teamId)
                    .category(category)
                    .labels(splitPipes(ordered.get(0).getStatLabels()))
                    .players(ordered.stream()
                            .map(player -> PlayerStatLineResponse.builder()
                                    .playerId(player.getKey().getPlayerId())
                                    .playerName(player.getPlayerName())
                                    .playerShortName(player.getPlayerShortName())
                                    .position(player.getPosition())
                                    .batOrder(player.getBatOrder())
                                    .starter(player.getStarter())
                                    .stats(splitPipes(player.getStatValues()))
                                    .build())
                            .toList())
                    .build());
        }));
        // Deterministic order: batting before pitching, then by team id.
        result.sort(Comparator.comparing(PlayerStatGroupResponse::getCategory)
                .thenComparing(PlayerStatGroupResponse::getTeamId));
        return result;
    }

    private List<String> splitPipes(String joined) {
        return joined == null ? List.of() : List.of(joined.split("\\|", -1));
    }

    // Polls ESPN only for leagues with a live (or due-to-start) game; when
    // nothing is live this is a single DB query and no ESPN calls.
    @Scheduled(fixedRate = 20000)
    public void updateGamesForToday() {
        List<League> leagues;
        try {
            leagues = gameRepository.findLeaguesNeedingUpdate().stream()
                    .map(League::valueOf)
                    .filter(league -> !league.isRetired())
                    .toList();
        } catch (Exception e) {
            logger.error("Could not check for live games: {}", e.getMessage());
            return;
        }
        if (leagues.isEmpty()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String yesterday = LocalDate.now().minusDays(1).format(formatter);
        String today = LocalDate.now().format(formatter);
        int updatedGameCount = 0;
        // Each league is isolated so one league's ESPN failure (rate limiting,
        // upstream errors, etc.) can't starve the rest of the leagues of updates.
        for (League league : leagues) {
            try {
                updatedGameCount += updateLiveGames(league, espnService.fetchGames(league, yesterday, today, false));
            } catch (Exception e) {
                logger.error("Error updating games for league {}:", league, e);
            }
        }
        logger.info("Updated {} games for {} in {}ms",
                updatedGameCount, leagues, System.currentTimeMillis() - startTime);
    }

    // The scoreboard is one request per day; the per-game summary is the
    // expensive part, so only fetch it for games that are playing or just
    // finished. Games already stored as final with stats are skipped entirely:
    // saving them without their summary would orphan-delete their stats. A
    // final game without stats (finished while the app was down, so the
    // schedule refresh stored it bare) still gets its summary fetched.
    private int updateLiveGames(League league, List<Game> games) {
        Map<Integer, Game> stored = gameRepository.findAllById(
                        games.stream().map(game -> (long) game.getId()).toList()).stream()
                .collect(Collectors.toMap(Game::getId, game -> game));

        List<Game> toSave = games.stream()
                .filter(game -> {
                    Game existing = stored.get(game.getId());
                    return existing == null
                            || !Boolean.TRUE.equals(existing.getFinalGame())
                            || existing.getStats() == null
                            || existing.getStats().isEmpty();
                })
                .toList();
        toSave.stream()
                .filter(game -> game.getGameStatus() != null
                        && !AWAITING_PLAY_STATUSES.contains(game.getGameStatus()))
                .forEach(game -> espnService.attachStatsToGame(game, league));
        gameRepository.saveAll(toSave);
        return toSave.size();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshSchedulesOnStartup() {
        refreshSchedules();
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "America/New_York")
    public void refreshSchedulesDaily() {
        refreshSchedules();
    }

    // Seeds/refreshes the rest of the current year's schedule for every league
    // so the 20s live-update job has upcoming games to trigger on. Schedule-only
    // (no per-game summary calls), but ESPN takes one date per request, so this
    // is one request per day per league. Past days are mostly skipped: games
    // that have started aren't saved here anyway; the short lookback only lets
    // pruning catch recently replaced placeholders.
    private void refreshSchedules() {
        long startTime = System.currentTimeMillis();
        LocalDate start = LocalDate.now().minusDays(SCHEDULE_LOOKBACK_DAYS);
        LocalDate end = LocalDate.of(LocalDate.now().getYear(), 12, 31);
        logger.info("Refreshing schedules for all leagues from {} to {}", start, end);
        for (League league : League.active()) {
            try {
                List<Game> games = espnService.fetchGamesForRange(league, start, end);
                int saved = saveScheduleGames(games);
                int pruned = pruneStaleScheduledGames(league, games, start, end);
                logger.info("Refreshed schedule for {}: saved {} of {} games, pruned {} stale",
                        league, saved, games.size(), pruned);
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

    // ESPN sometimes replaces a scheduled event with a new id (common with CFB
    // preseason placeholders), leaving the old id in our DB as a duplicate.
    // Delete scheduled games in the refreshed window that ESPN no longer lists.
    // An empty fetch skips pruning so a bad ESPN response can't wipe a schedule.
    // A stale game can still have child rows (odds, or stats from a game that
    // went live and was reset to scheduled), which are deleted along with it.
    // The window starts a day after the first fetched date: ESPN's scoreboard
    // days aren't UTC days, so a game near that edge may not have been fetched.
    private int pruneStaleScheduledGames(League league, List<Game> fetchedGames,
                                         LocalDate start, LocalDate end) {
        if (fetchedGames.isEmpty()) {
            return 0;
        }
        Instant windowStart = start.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant windowEnd = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Integer> fetchedIds = fetchedGames.stream().map(Game::getId).toList();
        List<Integer> staleIds = gameRepository.findStaleScheduledGameIds(
                league, windowStart, windowEnd, fetchedIds);
        if (staleIds.isEmpty()) {
            return 0;
        }
        logger.info("Pruning {} stale scheduled {} games: {}", staleIds.size(), league, staleIds);
        return deleteGames(staleIds);
    }

    // Children first, all in one transaction, so a failure leaves nothing half-deleted.
    private int deleteGames(List<Integer> ids) {
        Integer deleted = transactionTemplate.execute(status -> {
            gameRepository.deleteStatsByGameIds(ids);
            gameRepository.deleteLeadersByGameIds(ids);
            gameRepository.deleteScoringPlaysByGameIds(ids);
            gameRepository.deletePlayersByGameIds(ids);
            gameRepository.deleteOddsByGameIds(ids);
            return gameRepository.deleteGamesByIds(ids);
        });
        return deleted == null ? 0 : deleted;
    }
}
