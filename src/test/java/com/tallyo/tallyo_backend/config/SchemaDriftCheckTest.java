package com.tallyo.tallyo_backend.config;

import com.tallyo.tallyo_backend.entity.Game;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaDriftCheckTest {

    @Test
    void mapsTextAnnotatedFieldsToColumnNames() {
        List<String> columns = SchemaDriftCheck.textColumns(Game.class);
        assertTrue(columns.containsAll(List.of("last_play", "current_down_and_distance", "headline")), columns.toString());
        assertEquals("games", SchemaDriftCheck.tableName(Game.class));
    }
}
