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
import java.util.Locale;

/**
 * Logs an ERROR for every entity column annotated {@code columnDefinition = "text"} that is not text in the live
 * database. Hibernate's ddl-auto: update can narrow a text column back to varchar(255) and never widens it again
 * (issue #14), and the resulting "value too long" failures name no column.
 */
@Component
public class SchemaDriftCheck {
    private static final Logger logger = LoggerFactory.getLogger(SchemaDriftCheck.class);

    private final JdbcTemplate jdbcTemplate;
    private final EntityManagerFactory entityManagerFactory;

    public SchemaDriftCheck(JdbcTemplate jdbcTemplate, EntityManagerFactory entityManagerFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManagerFactory = entityManagerFactory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkOnStartup() {
        try {
            for (String drift : findDrift()) {
                logger.error("Schema drift: {}", drift);
            }
        } catch (Exception ex) {
            logger.warn("Could not check schema drift: {}", ex.getMessage());
        }
    }

    List<String> findDrift() {
        List<String> drift = new ArrayList<>();
        for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
            String table = tableName(entity.getJavaType());
            for (String column : textColumns(entity.getJavaType())) {
                List<String> types = jdbcTemplate.queryForList(
                        "SELECT data_type FROM information_schema.columns "
                                + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
                        String.class, table, column);
                if (!types.isEmpty() && !"text".equals(types.get(0))) {
                    drift.add(table + "." + column + " is " + types.get(0) + " in the database but text in "
                            + entity.getJavaType().getSimpleName() + "; add a db/migrations file with ALTER TABLE "
                            + table + " ALTER COLUMN " + column + " TYPE text");
                }
            }
        }
        return drift;
    }

    static List<String> textColumns(Class<?> type) {
        List<String> columns = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && "text".equalsIgnoreCase(column.columnDefinition())) {
                columns.add(column.name().isEmpty() ? snakeCase(field.getName()) : column.name());
            }
        }
        return columns;
    }

    static String tableName(Class<?> type) {
        Table table = type.getAnnotation(Table.class);
        return table != null && !table.name().isEmpty() ? table.name() : snakeCase(type.getSimpleName());
    }

    static String snakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
