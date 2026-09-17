package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SessionEvaluationResponse {

    private Long sessionId;

    // 2-3 sentence overall summary, in Vietnamese.
    private String summary;

    private String strengths;

    private String areasToImprove;

    // AI's best-effort CEFR estimate based on this one conversation -
    // informational only, does NOT automatically update LearnerLevel
    // (that always stays an explicit, user-confirmed action for now).
    private String estimatedLevel;
}
