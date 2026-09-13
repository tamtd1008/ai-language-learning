package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-readable label, e.g. "default-conversation", "grammar-checker".
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Which provider/service this config targets. Free-text on purpose -
    // the concrete AI client interfaces (AI-01+) will validate against
    // whatever providers are actually wired up at that point.
    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Builder.Default
    private Double temperature = 0.7;

    @Column(name = "max_tokens")
    @Builder.Default
    private Integer maxTokens = 1024;

    // Only one AiConfig is meant to be "active" (the one new conversation
    // sessions pick up by default) - enforced in AiConfigService, not at
    // the database level, so switching configs never requires a migration.
    @Column(nullable = false)
    private boolean active;

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
