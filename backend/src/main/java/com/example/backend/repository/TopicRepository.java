package com.example.backend.repository;

import com.example.backend.entity.CefrLevel;
import com.example.backend.entity.Topic;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByActiveTrue();

    Optional<Topic> findByName(String name);

    boolean existsByName(String name);

    // LEARN-08: topics at the learner's level they haven't started a
    // session for yet. excludedIds must be non-empty - see
    // findByActiveTrueAndLevel below for the "nothing to exclude" case.
    List<Topic> findByActiveTrueAndLevelAndIdNotIn(
            CefrLevel level,
            List<Long> excludedIds,
            Limit limit
    );

    List<Topic> findByActiveTrueAndLevel(CefrLevel level, Limit limit);
}
