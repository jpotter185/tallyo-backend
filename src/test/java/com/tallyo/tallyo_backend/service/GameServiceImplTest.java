package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceImplTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private EspnService espnService;

    private GameServiceImpl gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameServiceImpl(gameRepository, espnService);
    }

    @Test
    void testGetGames_Success() {
        // Arrange
        League league = League.NFL;
        int year = 2024;
        int seasonType = 1;
        int week = 1;
        Pageable pageable = PageRequest.of(0, 10);

        List<Game> games = new ArrayList<>();
        games.add(createTestGame(1L));
        games.add(createTestGame(2L));
        Page<Game> expectedPage = new PageImpl<>(games, pageable, 2);

        when(gameRepository.getGames(league, year, seasonType, week, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Game> result = gameService.getGames(league, year, seasonType, week, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(gameRepository, times(1)).getGames(league, year, seasonType, week, pageable);
    }

    @Test
    void testGetGames_EmptyResult() {
        // Arrange
        League league = League.CFB;
        int year = 2024;
        int seasonType = 2;
        int week = 5;
        Pageable pageable = PageRequest.of(0, 10);

        Page<Game> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        when(gameRepository.getGames(league, year, seasonType, week, pageable))
                .thenReturn(emptyPage);

        // Act
        Page<Game> result = gameService.getGames(league, year, seasonType, week, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(gameRepository, times(1)).getGames(league, year, seasonType, week, pageable);
    }

    @Test
    void testUpdateGames_Success() {
        // Arrange
        League league = League.NFL;
        int year = 2024;
        boolean shouldFetchStats = true;

        List<Game> fetchedGames = new ArrayList<>();
        fetchedGames.add(createTestGame(1L));
        fetchedGames.add(createTestGame(2L));

        when(espnService.fetchGames(league, year, shouldFetchStats))
                .thenReturn(fetchedGames);
        when(gameRepository.saveAll(fetchedGames))
                .thenReturn(fetchedGames);

        // Act
        List<Game> result = gameService.updateGames(league, year, shouldFetchStats);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(espnService, times(1)).fetchGames(league, year, shouldFetchStats);
        verify(gameRepository, times(1)).saveAll(fetchedGames);
    }

    @Test
    void testUpdateGames_NoStats() {
        // Arrange
        League league = League.CFB;
        int year = 2023;
        boolean shouldFetchStats = false;

        List<Game> fetchedGames = new ArrayList<>();
        fetchedGames.add(createTestGame(1L));

        when(espnService.fetchGames(league, year, shouldFetchStats))
                .thenReturn(fetchedGames);
        when(gameRepository.saveAll(fetchedGames))
                .thenReturn(fetchedGames);

        // Act
        List<Game> result = gameService.updateGames(league, year, shouldFetchStats);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(espnService, times(1)).fetchGames(league, year, shouldFetchStats);
        verify(gameRepository, times(1)).saveAll(fetchedGames);
    }

    @Test
    void testUpdateGames_EmptyResult() {
        // Arrange
        League league = League.NFL;
        int year = 2024;
        boolean shouldFetchStats = false;

        List<Game> emptyList = new ArrayList<>();
        when(espnService.fetchGames(league, year, shouldFetchStats))
                .thenReturn(emptyList);
        when(gameRepository.saveAll(emptyList))
                .thenReturn(emptyList);

        // Act
        List<Game> result = gameService.updateGames(league, year, shouldFetchStats);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(espnService, times(1)).fetchGames(league, year, shouldFetchStats);
        verify(gameRepository, times(1)).saveAll(emptyList);
    }

    @Test
    void testUpdateGamesForToday_ShouldNotUpdateWhenNoGamesInProgress() {
        // Arrange
        when(gameRepository.shouldUpdate()).thenReturn(false);

        // Act
        gameService.updateGamesForToday();

        // Assert
        verify(gameRepository, times(1)).shouldUpdate();
        verify(espnService, never()).fetchGames(any(), anyString(), anyString(), anyBoolean());
        verify(gameRepository, never()).saveAll(any());
    }

    @Test
    void testUpdateGamesForToday_ShouldUpdateWhenGamesInProgress() {
        // Arrange
        when(gameRepository.shouldUpdate()).thenReturn(true);

        List<Game> nflGames = new ArrayList<>();
        nflGames.add(createTestGame(1L));
        List<Game> cfbGames = new ArrayList<>();
        cfbGames.add(createTestGame(2L));

        when(espnService.fetchGames(eq(League.NFL), anyString(), anyString(), eq(true)))
                .thenReturn(nflGames);
        when(espnService.fetchGames(eq(League.CFB), anyString(), anyString(), eq(true)))
                .thenReturn(cfbGames);

        // Act
        gameService.updateGamesForToday();

        // Assert
        verify(gameRepository, times(1)).shouldUpdate();
        verify(espnService, times(2)).fetchGames(any(), anyString(), anyString(), eq(true));
        verify(gameRepository, times(2)).saveAll(any());
    }

    private Game createTestGame(Long id) {
        Game game = new Game();
        game.setId(Math.toIntExact(id));
        game.setLeague(League.NFL);
        return game;
    }
}
