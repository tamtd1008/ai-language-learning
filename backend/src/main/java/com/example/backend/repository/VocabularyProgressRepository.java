package com.example.backend.repository;

import com.example.backend.entity.VocabularyProgress;
import com.example.backend.entity.VocabularyProgressStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VocabularyProgressRepository extends JpaRepository<VocabularyProgress, Long> {

    Optional<VocabularyProgress> findByUserIdAndVocabularyId(Long userId, Long vocabularyId);

    List<VocabularyProgress> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<VocabularyProgress> findByUserIdAndStatusOrderByUpdatedAtDesc(
            Long userId,
            VocabularyProgressStatus status
    );

    long countByUserIdAndStatus(Long userId, VocabularyProgressStatus status);
}
