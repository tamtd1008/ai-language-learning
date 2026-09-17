package com.example.backend.service;

import com.example.backend.ai.AiJsonHelper;
import com.example.backend.ai.AiJsonHelper.AiJsonResult;
import com.example.backend.dto.request.AnalyzeAnswerRequest;
import com.example.backend.dto.response.AnalyzeAnswerResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.stereotype.Service;

/**
 * AI-07: analyzes a learner's answer to a given question - whether it
 * actually addresses the question, and the quality of the grammar/
 * vocabulary used - then suggests an improved version. Standalone (not
 * tied to a conversation session), so it can also be used for scripted
 * practice questions (LEARN-03+) later, not just free chat.
 */
@Service
@RequiredArgsConstructor
public class AnswerAnalysisService {

    private static final String SYSTEM_PROMPT =
            "You are analyzing a language learner's answer to a " +
                    "conversation question. Given the question and the " +
                    "learner's answer, evaluate: (1) whether the answer " +
                    "is on-topic and actually responds to the question, " +
                    "(2) the quality of grammar and vocabulary used, " +
                    "(3) how the answer could be improved. Respond with " +
                    "ONLY a single JSON object - no markdown code " +
                    "fences, no extra commentary - in exactly this " +
                    "shape: {\"onTopic\": <true or false>, \"feedback\": " +
                    "\"<2 to 4 sentence feedback in Vietnamese covering " +
                    "relevance, grammar and vocabulary>\", " +
                    "\"suggestedImprovement\": \"<a rewritten, improved " +
                    "version of the answer, in the same language as the " +
                    "original answer>\"}.";

    private final AiJsonHelper aiJsonHelper;

    public AnalyzeAnswerResponse analyze(AnalyzeAnswerRequest request) {

        String userMessage = "Question: " + request.getQuestion() +
                "\nLearner's answer: " + request.getAnswer();

        AiJsonResult<AnalyzeAnswerAiResult> result = aiJsonHelper.requestJson(
                SYSTEM_PROMPT,
                userMessage,
                0.3,
                AnalyzeAnswerAiResult.class
        );

        if (!result.isSuccess()) {

            return AnalyzeAnswerResponse.builder()
                    .question(request.getQuestion())
                    .answer(request.getAnswer())
                    .onTopic(true)
                    .feedback(result.getRaw())
                    .suggestedImprovement(request.getAnswer())
                    .build();
        }

        AnalyzeAnswerAiResult parsed = result.getValue();

        return AnalyzeAnswerResponse.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .onTopic(!Boolean.FALSE.equals(parsed.getOnTopic()))
                .feedback(
                        parsed.getFeedback() != null
                                ? parsed.getFeedback()
                                : ""
                )
                .suggestedImprovement(
                        parsed.getSuggestedImprovement() != null
                                ? parsed.getSuggestedImprovement()
                                : request.getAnswer()
                )
                .build();
    }

    // Internal shape of the AI's JSON reply.
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class AnalyzeAnswerAiResult {

        @JsonProperty("onTopic")
        private Boolean onTopic;

        private String feedback;

        private String suggestedImprovement;
    }
}
