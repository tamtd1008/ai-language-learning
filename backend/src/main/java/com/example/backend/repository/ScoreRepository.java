package com.example.backend.repository;

import com.example.backend.entity.Score;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScoreRepository extends JpaRepository<Score, Long> {

    Optional<Score> findBySessionId(Long sessionId);

    // PROGRESS-05: score history in session order, oldest first - the
    // trend a learner actually cares about ("am I improving?").
    List<Score> findBySession_User_IdOrderBySession_StartedAtAsc(Long userId);

    // PROGRESS-06: null (not 0) when the learner has no scored
    // sessions yet - handled explicitly in ProgressService rather than
    // defaulting to a misleading 0.
    @Query(
            "SELECT AVG(s.overallScore) FROM Score s " +
                    "WHERE s.session.user.id = :userId"
    )
    Double findAverageOverallScoreByUserId(@Param("userId") Long userId);
}
