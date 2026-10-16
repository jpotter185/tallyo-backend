package com.tallyo.tallyo_backend.config;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Logs an ERROR for every entity column annotated {@code columnDefinition = "text"} that is not {@code text} in the
 * live database. Hibernate's ddl-auto: update never narrows or widens columns, and Liquibase won't re-run an applied
 * changeset, so drift (issue #14) otherwise only shows up as "value too long" failures at game time.
 */
@Component
public class TextColumnDriftCheck {
    private static final Logger logger = LoggerFactory.getLogger(TextColumnDriftCheck.class);

    private final JdbcTemplate jdbcTemplate;
    private final EntityManagerFactory entityManagerFactory;

    public TextColumnDriftCheck(JdbcTemplate jdbcTemplate, EntityManagerFactory entityManagerFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManagerFactory = entityManagerFactory;
    }

    /** A column expected to be text: table name and column name. */
    public record TextColumn(String table, String column) {
    }

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        try {
            for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
                for (TextColumn expected : textColumns(entity.getJavaType())) {
                    List<String> types = jdbcTemplate.queryForList(
                            "SELECT data_type FROM information_schema.columns "
                                    + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
                            String.class, expected.table(), expected.column());
                    if (!types.isEmpty() && !"text".equals(types.get(0))) {
                        logger.error("Schema drift: {}.{} should be text but is {}; ESPN values over 255 chars "
                                        + "will fail saves. Add a Liquibase migration to widen it.",
                                expected.table(), expected.column(), types.get(0));
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Could not check text column drift: {}", ex.getMessage());
        }
    }

    static List<TextColumn> textColumns(Class<?> type) {
        Table table = type.getAnnotation(Table.class);
        String tableName = table != null && !table.name().isEmpty() ? table.name() : snakeCase(type.getSimpleName());
        List<TextColumn> result = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && "text".equalsIgnoreCase(column.columnDefinition())) {
                String name = column.name().isEmpty() ? snakeCase(field.getName()) : column.name();
                result.add(new TextColumn(tableName, name));
            }
        }
        return result;
    }

    private static String snakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
