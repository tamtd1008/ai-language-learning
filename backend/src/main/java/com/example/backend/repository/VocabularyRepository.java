package com.example.backend.repository;

import com.example.backend.entity.CefrLevel;
import com.example.backend.entity.Vocabulary;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    List<Vocabulary> findByActiveTrue();

    List<Vocabulary> findByActiveTrueAndTopicId(Long topicId);

    List<Vocabulary> findByActiveTrueAndLevel(CefrLevel level);

    List<Vocabulary> findByActiveTrueAndTopicIdAndLevel(Long topicId, CefrLevel level);

    // LEARN-08: vocabulary at the learner's level they haven't marked
    // LEARNED yet. excludedIds must be non-empty - see
    // findByActiveTrueAndLevel(CefrLevel, Limit) for the "nothing
    // learned yet" case.
    List<Vocabulary> findByActiveTrueAndLevelAndIdNotIn(
            CefrLevel level,
            List<Long> excludedIds,
            Limit limit
    );

    List<Vocabulary> findByActiveTrueAndLevel(CefrLevel level, Limit limit);
}
