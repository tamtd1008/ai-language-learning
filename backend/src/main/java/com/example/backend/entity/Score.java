package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One score per session - re-scoring updates this row rather than
    // creating a new one, enforced by the unique constraint on session_id.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private ConversationSession session;

    // All four 0-100. grammarScore/vocabularyScore/conversationScore
    // come from the AI; overallScore is computed in Java (their
    // average) rather than trusted from the AI, so it's always
    // arithmetically consistent with the other three.
    @Column(name = "grammar_score")
    private Double grammarScore;

    @Column(name = "vocabulary_score")
    private Double vocabularyScore;

    @Column(name = "conversation_score")
    private Double conversationScore;

    @Column(name = "overall_score")
    private Double overallScore;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
