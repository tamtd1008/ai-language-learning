package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vocabulary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Intentionally not unique - the same word can legitimately have
    // multiple entries (different meaning, topic or level), the way a
    // real dictionary would.
    @Column(nullable = false, length = 100)
    private String word;

    @Column(nullable = false, length = 500)
    private String meaning;

    @Column(length = 500)
    private String example;

    // IPA or simple phonetic spelling. Free-text for now - TTS-02/03
    // may later generate/validate this instead of relying on manual entry.
    @Column(length = 100)
    private String pronunciation;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CefrLevel level;

    // Nullable - a word doesn't have to belong to a specific topic.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(nullable = false)
    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
