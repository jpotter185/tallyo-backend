package com.tallyo.tallyo_backend.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeagueTest {

    @Test
    void testLeagueNFLValue() {
        // Act & Assert
        assertEquals("nfl", League.NFL.getValue());
    }

    @Test
    void testLeagueCFBValue() {
        // Act & Assert
        assertEquals("college-football", League.CFB.getValue());
    }

    @Test
    void testLeagueNFLEnum() {
        // Act & Assert
        assertNotNull(League.NFL);
        assertEquals(League.NFL, League.valueOf("NFL"));
    }

    @Test
    void testLeagueCFBEnum() {
        // Act & Assert
        assertNotNull(League.CFB);
        assertEquals(League.CFB, League.valueOf("CFB"));
    }

    @Test
    void testLeagueValues() {
        // Act
        League[] leagues = League.values();

        // Assert
        assertEquals(2, leagues.length);
        assertArrayEquals(new League[]{League.NFL, League.CFB}, leagues);
    }

    @Test
    void testLeagueInvalidEnum() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            League.valueOf("INVALID");
        });
    }
}
