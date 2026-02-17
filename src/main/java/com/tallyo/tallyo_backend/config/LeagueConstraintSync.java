package com.tallyo.tallyo_backend.config;

import com.tallyo.tallyo_backend.enums.League;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class LeagueConstraintSync {
    private static final Logger logger = LoggerFactory.getLogger(LeagueConstraintSync.class);

    private final JdbcTemplate jdbcTemplate;

    public LeagueConstraintSync(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void syncLeagueConstraints() {
        String allowed = Arrays.stream(League.values())
                .map(Enum::name)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));

        refreshConstraint("teams", "league", "teams_league_check", allowed);
        refreshConstraint("games", "league", "games_league_check", allowed);
    }

    private void refreshConstraint(String table, String column, String constraintName, String allowedValuesSql) {
        String dropSql = "ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraintName;
        String addSql = "ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName
                + " CHECK (" + column + " IN (" + allowedValuesSql + "))";

        try {
            jdbcTemplate.execute(dropSql);
            jdbcTemplate.execute(addSql);
            logger.info("Synchronized {} on {}", constraintName, table);
        } catch (Exception ex) {
            logger.warn("Could not synchronize {} on {}: {}", constraintName, table, ex.getMessage());
        }
    }
}
