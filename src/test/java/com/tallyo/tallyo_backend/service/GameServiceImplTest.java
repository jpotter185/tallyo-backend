package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.GameStat;
import com.tallyo.tallyo_backend.entity.GameStatKey;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GameServiceImplTest {

    private final GameRepository gameRepository = mock(GameRepository.class);
    private final EspnService espnService = mock(EspnService.class);
    private final GameServiceImpl service = new GameServiceImpl(gameRepository, espnService);

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

    private Game game(int id, String status, boolean finalGame) {
        return Game.builder().id(id).league(League.MLB).gameStatus(status).finalGame(finalGame).build();
    }
}
