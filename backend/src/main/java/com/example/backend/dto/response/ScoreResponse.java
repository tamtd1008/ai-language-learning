package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ScoreResponse {

    private Long sessionId;

    private Double grammarScore;

    private Double vocabularyScore;

    private Double conversationScore;

    // Average of the three scores above, computed in Java (not by the
    // AI) so it's always arithmetically consistent.
    private Double overallScore;

    private LocalDateTime createdAt;
}
