package com.tallyo.tallyo_backend.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateResponseTest {

    @Test
    void testUpdateResponse_Creation() {
        // Arrange
        int gameCount = 5;
        Long durationMs = 1000L;

        // Act
        UpdateResponse response = new UpdateResponse(gameCount, durationMs);

        // Assert
        assertNotNull(response);
        assertEquals(gameCount, response.gameCount());
        assertEquals(durationMs, response.durationMs());
    }

    @Test
    void testUpdateResponse_ZeroGames() {
        // Arrange
        int gameCount = 0;
        Long durationMs = 100L;

        // Act
        UpdateResponse response = new UpdateResponse(gameCount, durationMs);

        // Assert
        assertEquals(0, response.gameCount());
        assertEquals(100L, response.durationMs());
    }

    @Test
    void testUpdateResponse_LargeDuration() {
        // Arrange
        int gameCount = 50;
        Long durationMs = 5000L;

        // Act
        UpdateResponse response = new UpdateResponse(gameCount, durationMs);

        // Assert
        assertEquals(50, response.gameCount());
        assertEquals(5000L, response.durationMs());
    }

    @Test
    void testUpdateResponse_Equality() {
        // Arrange
        UpdateResponse response1 = new UpdateResponse(10, 1000L);
        UpdateResponse response2 = new UpdateResponse(10, 1000L);
        UpdateResponse response3 = new UpdateResponse(5, 500L);

        // Act & Assert
        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
    }
}
