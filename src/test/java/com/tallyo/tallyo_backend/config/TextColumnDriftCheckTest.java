package com.tallyo.tallyo_backend.config;

import com.tallyo.tallyo_backend.config.TextColumnDriftCheck.TextColumn;
import com.tallyo.tallyo_backend.entity.Game;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TextColumnDriftCheckTest {
    @Test
    void mapsTextAnnotatedFieldsToSnakeCaseColumns() {
        List<TextColumn> columns = TextColumnDriftCheck.textColumns(Game.class);
        assertTrue(columns.contains(new TextColumn("games", "last_play")));
        assertTrue(columns.contains(new TextColumn("games", "current_down_and_distance")));
        assertTrue(columns.contains(new TextColumn("games", "headline")));
    }
}
