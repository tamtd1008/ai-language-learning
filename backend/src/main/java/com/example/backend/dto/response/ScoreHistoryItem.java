package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ScoreHistoryItem {

    private Long sessionId;

    private LocalDateTime startedAt;

    private Double overallScore;
}
