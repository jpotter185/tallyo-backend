package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.GameStat;
import com.tallyo.tallyo_backend.entity.GameStatKey;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GameServiceImplTest {

    private final GameRepository gameRepository = mock(GameRepository.class);
    private final EspnService espnService = mock(EspnService.class);
    private final GameServiceImpl service = new GameServiceImpl(gameRepository, espnService,
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

    @Test
    void noLiveGamesMakesNoEspnCalls() {
        when(gameRepository.findLeaguesNeedingUpdate()).thenReturn(List.of());

        service.updateGamesForToday();

        verifyNoInteractions(espnService);
    }

    @Test
    void onlyLeaguesWithLiveGamesAreFetched() {
        when(gameRepository.findLeaguesNeedingUpdate()).thenReturn(List.of("MLB"));
        when(espnService.fetchGames(any(League.class), anyString(), anyString(), anyBoolean()))
                .thenReturn(List.of());

        service.updateGamesForToday();

        verify(espnService).fetchGames(eq(League.MLB), anyString(), anyString(), eq(false));
        verify(espnService, never()).fetchGames(eq(League.NFL), anyString(), anyString(), anyBoolean());
    }

    @Test
    void retiredLeagueIsNeverPolled() {
        when(gameRepository.findLeaguesNeedingUpdate()).thenReturn(List.of("WORLD_CUP"));

        service.updateGamesForToday();

        verifyNoInteractions(espnService);
    }

    @Test
    void retiredLeagueIsHiddenButStillStorable() {
        assertThat(League.active()).doesNotContain(League.WORLD_CUP).contains(League.NFL, League.MLB);
        assertThat(League.valueOf("WORLD_CUP").isRetired()).isTrue();
        org.junit.jupiter.api.Assertions.assertThrows(
                com.tallyo.tallyo_backend.exception.InvalidRequestException.class,
                () -> service.updateGames(League.WORLD_CUP, 2026, false));
        verifyNoInteractions(espnService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void summariesOnlyFetchedForLiveOrNewlyFinalGames() {
        Game live = game(1, "STATUS_IN_PROGRESS", false);
        Game justFinished = game(2, "STATUS_FINAL", true);
        Game alreadyFinal = game(3, "STATUS_FINAL", true);
        Game scheduled = game(4, "STATUS_SCHEDULED", false);
        Game finalWithoutStats = game(5, "STATUS_FINAL", true);
        Game storedFinal = game(3, "STATUS_FINAL", true);
        storedFinal.addStats(List.of(GameStat.builder()
                .key(new GameStatKey(3, 10, "hits")).statValue("8").build()));
        when(gameRepository.findLeaguesNeedingUpdate()).thenReturn(List.of("MLB"));
        when(espnService.fetchGames(eq(League.MLB), anyString(), anyString(), eq(false)))
                .thenReturn(List.of(live, justFinished, alreadyFinal, scheduled, finalWithoutStats));
        when(gameRepository.findAllById(any())).thenReturn(List.of(
                game(1, "STATUS_IN_PROGRESS", false),
                game(2, "STATUS_IN_PROGRESS", false),
                storedFinal,
                game(4, "STATUS_SCHEDULED", false),
                game(5, "STATUS_FINAL", true)));

        service.updateGamesForToday();

        verify(espnService).attachStatsToGame(live, League.MLB);
        verify(espnService).attachStatsToGame(justFinished, League.MLB);
        verify(espnService, never()).attachStatsToGame(alreadyFinal, League.MLB);
        verify(espnService, never()).attachStatsToGame(scheduled, League.MLB);
        verify(espnService).attachStatsToGame(finalWithoutStats, League.MLB);

        ArgumentCaptor<List<Game>> saved = ArgumentCaptor.forClass(List.class);
        verify(gameRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).containsExactly(live, justFinished, scheduled, finalWithoutStats);
    }

    @Test
    void staleScheduledGamesAreDeletedAfterTheirChildRows() {
        when(espnService.fetchGamesForRange(eq(League.MLB), any(), any()))
                .thenReturn(List.of(game(1, "STATUS_SCHEDULED", false)));
        when(gameRepository.findStaleScheduledGameIds(eq(League.MLB), any(), any(), eq(List.of(1))))
                .thenReturn(List.of(99));

        service.refreshSchedulesDaily();

        InOrder order = inOrder(gameRepository);
        order.verify(gameRepository).deleteStatsByGameIds(List.of(99));
        order.verify(gameRepository).deleteLeadersByGameIds(List.of(99));
        order.verify(gameRepository).deleteScoringPlaysByGameIds(List.of(99));
        order.verify(gameRepository).deletePlayersByGameIds(List.of(99));
        order.verify(gameRepository).deleteOddsByGameIds(List.of(99));
        order.verify(gameRepository).deleteGamesByIds(List.of(99));
    }

    @Test
    void noStaleGamesDeletesNothing() {
        when(espnService.fetchGamesForRange(eq(League.MLB), any(), any()))
                .thenReturn(List.of(game(1, "STATUS_SCHEDULED", false)));
        when(gameRepository.findStaleScheduledGameIds(any(), any(), any(), any())).thenReturn(List.of());

        service.refreshSchedulesDaily();

        verify(gameRepository, never()).deleteStatsByGameIds(anyList());
        verify(gameRepository, never()).deleteGamesByIds(anyList());
    }

    private Game game(int id, String status, boolean finalGame) {
        return Game.builder().id(id).league(League.MLB).gameStatus(status).finalGame(finalGame).build();
    }
}
