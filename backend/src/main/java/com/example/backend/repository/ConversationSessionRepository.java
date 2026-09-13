package com.example.backend.repository;

import com.example.backend.entity.ConversationSession;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {

    List<ConversationSession> findByUserIdOrderByStartedAtDesc(Long userId);

    // Used to enforce ownership in one query instead of "find then check
    // owner in Java" - also means a wrong-owner lookup and a genuinely
    // missing id are indistinguishable (both empty), which is what we
    // want: see ConversationSessionService's 404-not-403 comment.
    Optional<ConversationSession> findByIdAndUserUsername(Long id, String username);
}
