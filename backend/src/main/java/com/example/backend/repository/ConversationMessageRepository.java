package com.example.backend.repository;

import com.example.backend.entity.ConversationMessage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
