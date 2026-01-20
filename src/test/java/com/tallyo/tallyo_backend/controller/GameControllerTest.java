package com.tallyo.tallyo_backend.controller;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.dto.PageResponse;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.service.CalendarServiceImpl;
import com.tallyo.tallyo_backend.service.GameServiceImpl;
import org.apache.coyote.BadRequestException;
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
class GameControllerTest {

    @Mock
    private GameServiceImpl gameServiceImpl;

    @Mock
    private CalendarServiceImpl calendarService;

    private GameController gameController;

    @BeforeEach
    void setUp() {
        gameController = new GameController(gameServiceImpl, calendarService);
    }

    @Test
    void testGetCurrentContext_Success() throws BadRequestException {
        // Arrange
        String league = "NFL";
        CurrentContext expectedContext = new CurrentContext(2024, 1, 5);
        when(calendarService.getCurrentContext(League.NFL))
                .thenReturn(expectedContext);

        // Act
        CurrentContext result = gameController.getCurrentContext(league);

        // Assert
        assertNotNull(result);
        assertEquals(expectedContext.year(), result.year());
        verify(calendarService, times(1)).getCurrentContext(League.NFL);
    }

    @Test
    void testGetCurrentContext_InvalidLeague() {
        // Arrange
        String invalidLeague = "INVALID";

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            gameController.getCurrentContext(invalidLeague);
        });
        verify(calendarService, never()).getCurrentContext(any());
    }

    @Test
    void testGetCurrentContext_CaseInsensitive() throws BadRequestException {
        // Arrange
        String league = "nfl";
        CurrentContext expectedContext = new CurrentContext(2024, 1, 5);
        when(calendarService.getCurrentContext(League.NFL))
                .thenReturn(expectedContext);

        // Act
        CurrentContext result = gameController.getCurrentContext(league);

        // Assert
        assertNotNull(result);
        verify(calendarService, times(1)).getCurrentContext(League.NFL);
    }

    @Test
    void testGetGames_Success() throws BadRequestException {
        // Arrange
        String league = "NFL";
        Integer year = 2024;
        Integer seasonType = 1;
        Integer week = 1;
        Integer size = 10;
        Integer page = 0;
        String sortBy = "id";

        List<Game> gameList = new ArrayList<>();
        gameList.add(createTestGame(1L));
        gameList.add(createTestGame(2L));
        Pageable pageable = PageRequest.of(page, size);
        Page<Game> gamePages = new PageImpl<>(gameList, pageable, 2);

        when(gameServiceImpl.getGames(eq(League.NFL), eq(year), eq(seasonType), eq(week), any(Pageable.class)))
                .thenReturn(gamePages);

        // Act
        PageResponse<Game> result = gameController.getGames(league, year, seasonType, week, size, page, sortBy);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(gameServiceImpl, times(1)).getGames(eq(League.NFL), eq(year), eq(seasonType), eq(week), any(Pageable.class));
    }

    @Test
    void testGetGames_WithDefaultValues() throws BadRequestException {
        // Arrange
        String league = "CFB";
        Integer year = 0; // default
        Integer seasonType = 0; // default
        Integer week = 0; // default
        Integer size = 100; // default
        Integer page = 0; // default
        String sortBy = "id"; // default

        List<Game> gameList = new ArrayList<>();
        Pageable pageable = PageRequest.of(page, size);
        Page<Game> gamePages = new PageImpl<>(gameList, pageable, 0);

        when(gameServiceImpl.getGames(eq(League.CFB), eq(year), eq(seasonType), eq(week), any(Pageable.class)))
                .thenReturn(gamePages);

        // Act
        PageResponse<Game> result = gameController.getGames(league, year, seasonType, week, size, page, sortBy);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(gameServiceImpl, times(1)).getGames(eq(League.CFB), eq(year), eq(seasonType), eq(week), any(Pageable.class));
    }

    @Test
    void testGetGames_InvalidLeague() {
        // Arrange
        String invalidLeague = "INVALID_LEAGUE";
        Integer year = 2024;
        Integer seasonType = 1;
        Integer week = 1;
        Integer size = 10;
        Integer page = 0;
        String sortBy = "id";

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            gameController.getGames(invalidLeague, year, seasonType, week, size, page, sortBy);
        });
        verify(gameServiceImpl, never()).getGames(any(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void testGetGames_EmptyResult() throws BadRequestException {
        // Arrange
        String league = "NFL";
        Integer year = 2024;
        Integer seasonType = 1;
        Integer week = 1;
        Integer size = 10;
        Integer page = 0;
        String sortBy = "id";

        List<Game> emptyList = new ArrayList<>();
        Pageable pageable = PageRequest.of(page, size);
        Page<Game> emptyPage = new PageImpl<>(emptyList, pageable, 0);

        when(gameServiceImpl.getGames(eq(League.NFL), eq(year), eq(seasonType), eq(week), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PageResponse<Game> result = gameController.getGames(league, year, seasonType, week, size, page, sortBy);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(gameServiceImpl, times(1)).getGames(eq(League.NFL), eq(year), eq(seasonType), eq(week), any(Pageable.class));
    }

    @Test
    void testGetCurrentWeekGames_Success() throws BadRequestException {
        // Arrange
        String league = "NFL";
        Integer year = null;
        Integer seasonType = null;
        Integer week = null;
        Integer size = 100;
        Integer page = 0;
        String sortBy = "id";

        CurrentContext currentContext = new CurrentContext(2024, 1, 5);
        when(calendarService.getCurrentContext(League.NFL))
                .thenReturn(currentContext);
        when(calendarService.getCurrentYear())
                .thenReturn(2024);

        List<Game> gameList = new ArrayList<>();
        gameList.add(createTestGame(1L));
        Pageable pageable = PageRequest.of(page, size);
        Page<Game> gamePages = new PageImpl<>(gameList, pageable, 1);

        when(gameServiceImpl.getGames(eq(League.NFL), eq(2024), eq(1), eq(5), any(Pageable.class)))
                .thenReturn(gamePages);

        // Act
        PageResponse<Game> result = gameController.getCurrentWeekGames(league, year, seasonType, week, size, page, sortBy);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(calendarService, times(1)).getCurrentContext(League.NFL);
        verify(calendarService, times(1)).getCurrentYear();
        verify(gameServiceImpl, times(1)).getGames(eq(League.NFL), eq(2024), eq(1), eq(5), any(Pageable.class));
    }

    @Test
    void testGetCurrentWeekGames_InvalidLeague() {
        // Arrange
        String invalidLeague = "INVALID";
        Integer year = null;
        Integer seasonType = null;
        Integer week = null;
        Integer size = 100;
        Integer page = 0;
        String sortBy = "id";

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            gameController.getCurrentWeekGames(invalidLeague, year, seasonType, week, size, page, sortBy);
        });
        verify(calendarService, never()).getCurrentContext(any());
    }

    @Test
    void testUpdateGames_Success() throws BadRequestException {
        // Arrange
        String league = "NFL";
        int year = 2024;
        boolean shouldFetchStats = true;

        List<Game> updatedGames = new ArrayList<>();
        updatedGames.add(createTestGame(1L));
        updatedGames.add(createTestGame(2L));

        when(gameServiceImpl.updateGames(League.NFL, year, shouldFetchStats))
                .thenReturn(updatedGames);

        // Act
        var result = gameController.updateGames(league, year, shouldFetchStats);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.gameCount());
        verify(gameServiceImpl, times(1)).updateGames(League.NFL, year, shouldFetchStats);
    }

    @Test
    void testUpdateGames_InvalidLeague() {
        // Arrange
        String invalidLeague = "INVALID";
        int year = 2024;
        boolean shouldFetchStats = false;

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            gameController.updateGames(invalidLeague, year, shouldFetchStats);
        });
        verify(gameServiceImpl, never()).updateGames(any(), anyInt(), anyBoolean());
    }

    @Test
    void testUpdateGames_WithDefaultYear() throws BadRequestException {
        // Arrange
        String league = "CFB";
        int year = 0; // default
        boolean shouldFetchStats = false;

        List<Game> updatedGames = new ArrayList<>();
        when(gameServiceImpl.updateGames(League.CFB, year, shouldFetchStats))
                .thenReturn(updatedGames);

        // Act
        var result = gameController.updateGames(league, year, shouldFetchStats);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.gameCount());
        verify(gameServiceImpl, times(1)).updateGames(League.CFB, year, shouldFetchStats);
    }

    @Test
    void testUpdateGames_EmptyResult() throws BadRequestException {
        // Arrange
        String league = "NFL";
        int year = 2024;
        boolean shouldFetchStats = true;

        List<Game> emptyList = new ArrayList<>();
        when(gameServiceImpl.updateGames(League.NFL, year, shouldFetchStats))
                .thenReturn(emptyList);

        // Act
        var result = gameController.updateGames(league, year, shouldFetchStats);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.gameCount());
        verify(gameServiceImpl, times(1)).updateGames(League.NFL, year, shouldFetchStats);
    }

    private Game createTestGame(Long id) {
        Game game = new Game();
        game.setId(Math.toIntExact(id));
        game.setLeague(League.NFL);
        return game;
    }
}
