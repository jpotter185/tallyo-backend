package com.tallyo.tallyo_backend.repository;

import com.tallyo.tallyo_backend.dto.CurrentContext;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.enums.League;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByLeague(League league);

    @Query("SELECT g FROM Game g WHERE g.league = :league " +
            "AND (:year = 0 OR g.year = :year) " +
            "AND (:seasonType = 0 OR g.seasonType = :seasonType) " +
            "AND (:week = 0 OR g.week = :week)" +
            "ORDER BY CASE " +
            "  WHEN g.gameStatus = 'STATUS_IN_PROGRESS' OR  g.gameStatus = 'STATUS_END_PERIOD'  OR  g.gameStatus = 'STATUS_HALFTIME' THEN 1 " +
            "  WHEN g.gameStatus = 'STATUS_SCHEDULED' THEN 2 " +
            "  WHEN g.gameStatus = 'STATUS_FINAL' THEN 3 " +
            "  ELSE 4 " +
            "END, g.isoDate")
    Page<Game> getGames(@Param("league") League league,
                        @Param("year") int year,
                        @Param("seasonType") int seasonType,
                        @Param("week") int week,
                        Pageable pageable);


    @Query("SELECT new com.tallyo.tallyo_backend.dto.CurrentContext(g.year, g.seasonType, g.week) " +
            "FROM Game g WHERE g.league = :league " +
            "AND g.finalGame = true " +
            "ORDER BY g.isoDate DESC, g.id DESC " +
            "LIMIT 1")
    CurrentContext findCurrentContext(@Param("league") League league);

    @Query(value = "SELECT COUNT(*) > 0 " +
            "FROM games " +
            "WHERE iso_date::timestamp >= NOW() - INTERVAL '24 hours' " +
            "AND (" +
            "  game_status IN ('STATUS_IN_PROGRESS', 'STATUS_HALFTIME') " +
            "  OR " +
            "  (game_status = 'STATUS_SCHEDULED' AND iso_date::timestamp <= NOW())" +
            ")", nativeQuery = true)
    boolean shouldUpdate();
}
