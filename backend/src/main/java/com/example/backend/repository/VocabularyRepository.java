package com.example.backend.repository;

import com.example.backend.entity.CefrLevel;
import com.example.backend.entity.Vocabulary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    List<Vocabulary> findByActiveTrue();

    List<Vocabulary> findByActiveTrueAndTopicId(Long topicId);

    List<Vocabulary> findByActiveTrueAndLevel(CefrLevel level);

    List<Vocabulary> findByActiveTrueAndTopicIdAndLevel(Long topicId, CefrLevel level);
}
