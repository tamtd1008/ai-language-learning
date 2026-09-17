package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AnalyzeAnswerResponse {

    private String question;

    private String answer;

    // Whether the answer actually responds to the question asked.
    private boolean onTopic;

    // Vietnamese feedback covering relevance, grammar and vocabulary.
    private String feedback;

    // A rewritten, improved version of the learner's answer.
    private String suggestedImprovement;
}
