package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class VocabularyResponse {

    private Long id;

    private String word;

    private String meaning;

    private String example;

    private String pronunciation;

    private String level;

    private Long topicId;

    private String topicName;

    private boolean active;

    private LocalDateTime createdAt;
}
