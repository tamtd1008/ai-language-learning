package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProgressSummaryResponse {

    private long totalSessions;

    // Messages the learner actually sent - AI replies aren't counted.
    private long totalMessagesSent;

    // Null when no session has been scored yet - not 0, which would
    // misleadingly look like a real (bad) score.
    private Double averageOverallScore;

    // Null when the learner hasn't set a level yet (LEARN-01).
    private String currentLevel;

    private long vocabularyLearnedCount;

    private long vocabularyLearningCount;

    // Oldest first - a learner (or a chart on the frontend) reads this
    // as "progress over time" (PROGRESS-05).
    private List<ScoreHistoryItem> scoreHistory;
}
