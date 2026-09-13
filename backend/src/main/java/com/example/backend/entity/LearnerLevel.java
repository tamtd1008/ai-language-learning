package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "learner_levels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnerLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One level record per user - enforced by a unique constraint on
    // user_id, not just convention.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LevelSource source;

    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private CefrLevel level;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
