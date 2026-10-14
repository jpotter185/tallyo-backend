package com.tallyo.tallyo_backend.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** ESPN free text has no length limit, so these must not default to varchar(255) (issues #4, #12). */
class TextColumnsTest {

    private static void assertText(Class<?> type, String field) throws NoSuchFieldException {
        Column column = type.getDeclaredField(field).getAnnotation(Column.class);
        assertEquals("text", column == null ? null : column.columnDefinition(), type.getSimpleName() + "." + field);
    }

    @Test
    void freeTextFieldsAreTextColumns() throws Exception {
        for (String f : new String[]{"lastPlay", "currentDownAndDistance", "headline", "stadiumName", "location",
                "channel", "espnLink"}) {
            assertText(Game.class, f);
        }
        assertText(GamePlayer.class, "statLabels");
        assertText(GamePlayer.class, "statValues");
        assertText(ScoringPlay.class, "displayText");
        assertText(GameStat.class, "statValue");
        assertText(GameLeader.class, "displayValue");
    }
}
