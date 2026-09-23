package com.tallyo.tallyo_backend.repository;

import com.tallyo.tallyo_backend.entity.TeamInjury;
import com.tallyo.tallyo_backend.entity.TeamInjuryKey;
import com.tallyo.tallyo_backend.enums.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamInjuryRepository extends JpaRepository<TeamInjury, TeamInjuryKey> {

    @Query("SELECT t FROM TeamInjury t WHERE t.key.league = :league ORDER BY t.key.teamId, t.athleteName")
    List<TeamInjury> findByLeague(@Param("league") League league);

    @Modifying
    @Query("DELETE FROM TeamInjury t WHERE t.key.league = :league AND t.key.teamId = :teamId")
    int deleteByTeam(@Param("league") League league, @Param("teamId") Integer teamId);
}
