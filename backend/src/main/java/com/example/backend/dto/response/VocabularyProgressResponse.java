package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class VocabularyProgressResponse {

    private Long vocabularyId;

    private String word;

    private String meaning;

    private String status;

    private LocalDateTime updatedAt;
}
