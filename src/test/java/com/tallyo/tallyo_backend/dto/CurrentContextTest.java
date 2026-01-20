package com.tallyo.tallyo_backend.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrentContextTest {

    @Test
    void testCurrentContext_Creation() {
        // Arrange
        int year = 2024;
        int seasonType = 1;
        int week = 5;

        // Act
        CurrentContext context = new CurrentContext(year, seasonType, week);

        // Assert
        assertNotNull(context);
        assertEquals(year, context.year());
        assertEquals(seasonType, context.seasonType());
        assertEquals(week, context.week());
    }

    @Test
    void testCurrentContext_Getters() {
        // Arrange
        CurrentContext context = new CurrentContext(2023, 2, 10);

        // Act & Assert
        assertEquals(2023, context.year());
        assertEquals(2, context.seasonType());
        assertEquals(10, context.week());
    }

    @Test
    void testCurrentContext_Equality() {
        // Arrange
        CurrentContext context1 = new CurrentContext(2024, 1, 5);
        CurrentContext context2 = new CurrentContext(2024, 1, 5);
        CurrentContext context3 = new CurrentContext(2023, 2, 5);

        // Act & Assert
        assertEquals(context1, context2);
        assertNotEquals(context1, context3);
    }

    @Test
    void testCurrentContext_ZeroValues() {
        // Arrange
        CurrentContext context = new CurrentContext(0, 0, 0);

        // Act & Assert
        assertEquals(0, context.year());
        assertEquals(0, context.seasonType());
        assertEquals(0, context.week());
    }

    @Test
    void testCurrentContext_NegativeValues() {
        // Arrange - Records allow negative values
        CurrentContext context = new CurrentContext(-1, -1, -1);

        // Act & Assert
        assertEquals(-1, context.year());
        assertEquals(-1, context.seasonType());
        assertEquals(-1, context.week());
    }

    @Test
    void testCurrentContext_HashCode() {
        // Arrange
        CurrentContext context1 = new CurrentContext(2024, 1, 5);
        CurrentContext context2 = new CurrentContext(2024, 1, 5);

        // Act & Assert
        assertEquals(context1.hashCode(), context2.hashCode());
    }
}
