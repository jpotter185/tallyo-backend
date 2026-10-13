package com.tallyo.tallyo_backend.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameColumnTest {

    // ESPN play text and headlines can exceed varchar(255), which fails the whole league update.
    @Test
    void freeTextFieldsAreUnbounded() throws NoSuchFieldException {
        for (String field : new String[]{"lastPlay", "currentDownAndDistance", "headline"}) {
            Column column = Game.class.getDeclaredField(field).getAnnotation(Column.class);
            assertEquals("text", column.columnDefinition(), field);
        }
    }
}
