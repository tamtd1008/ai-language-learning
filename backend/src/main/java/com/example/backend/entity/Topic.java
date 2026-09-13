package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // Difficulty this topic is aimed at. Nullable - some topics
    // (e.g. "Small talk") are reasonable at any level.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CefrLevel level;

    // Soft toggle so admins can hide a topic without deleting history
    // that already references it (conversation sessions, progress...).
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
