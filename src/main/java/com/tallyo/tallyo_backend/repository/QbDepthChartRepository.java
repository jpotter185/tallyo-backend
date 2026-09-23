package com.tallyo.tallyo_backend.repository;

import com.tallyo.tallyo_backend.entity.QbDepthChartEntry;
import com.tallyo.tallyo_backend.entity.QbDepthChartKey;
import com.tallyo.tallyo_backend.enums.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QbDepthChartRepository extends JpaRepository<QbDepthChartEntry, QbDepthChartKey> {

    @Query("SELECT q FROM QbDepthChartEntry q WHERE q.key.league = :league ORDER BY q.key.teamId, q.key.depthRank")
    List<QbDepthChartEntry> findByLeague(@Param("league") League league);

    @Modifying
    @Query("DELETE FROM QbDepthChartEntry q WHERE q.key.league = :league AND q.key.teamId = :teamId")
    int deleteByTeam(@Param("league") League league, @Param("teamId") Integer teamId);
}
