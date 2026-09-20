package com.example.backend.repository;

import com.example.backend.entity.ConversationMessage;
import com.example.backend.entity.MessageRole;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    // PROGRESS-06: total messages the learner has actually sent (not
    // counting AI replies), across every one of their sessions.
    long countBySession_User_IdAndRole(Long userId, MessageRole role);
}
