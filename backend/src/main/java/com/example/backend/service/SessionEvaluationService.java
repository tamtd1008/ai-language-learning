package com.example.backend.service;

import com.example.backend.ai.AiJsonHelper;
import com.example.backend.ai.AiJsonHelper.AiJsonResult;
import com.example.backend.dto.response.SessionEvaluationResponse;
import com.example.backend.entity.ConversationMessage;
import com.example.backend.entity.ConversationSession;
import com.example.backend.entity.MessageRole;
import com.example.backend.entity.SessionStatus;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ConversationMessageRepository;
import com.example.backend.repository.ConversationSessionRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI-08: qualitative evaluation of a completed conversation session -
 * distinct from SCORE-01..04 (numeric scoring, a later module), this
 * produces a short written assessment: what went well, what to work on,
 * and the AI's best-effort CEFR estimate for this one conversation.
 * Only works on ENDED sessions - evaluating a still-active session
 * would be judging an unfinished conversation.
 */
@Service
@RequiredArgsConstructor
public class SessionEvaluationService {

    private static final String SYSTEM_PROMPT =
            "You are evaluating a completed language-practice " +
                    "conversation between a learner and an AI tutor. " +
                    "You will be given the full transcript, with each " +
                    "line prefixed by who said it. Assess the learner's " +
                    "performance: grammar accuracy, vocabulary range, " +
                    "fluency (based on sentence complexity and response " +
                    "length), and topic engagement. Respond with ONLY a " +
                    "single JSON object - no markdown code fences, no " +
                    "extra commentary - in exactly this shape: " +
                    "{\"summary\": \"<2 to 3 sentence overall summary, " +
                    "in Vietnamese>\", \"strengths\": \"<what the " +
                    "learner did well, in Vietnamese>\", " +
                    "\"areasToImprove\": \"<concrete, specific things to " +
                    "work on, in Vietnamese>\", \"estimatedLevel\": " +
                    "\"<your best estimate of the learner's CEFR level " +
                    "based on this conversation - must be exactly one " +
                    "of A1, A2, B1, B2, C1, C2>\"}.";

    private final ConversationSessionRepository sessionRepository;

    private final ConversationMessageRepository messageRepository;

    private final AiJsonHelper aiJsonHelper;

    public SessionEvaluationResponse evaluate(String username, Long sessionId) {

        ConversationSession session = sessionRepository
                .findByIdAndUserUsername(sessionId, username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Session not found"
                        )
                );

        if (session.getStatus() != SessionStatus.ENDED) {

            throw new BadRequestException(
                    "Only ended sessions can be evaluated - end the " +
                            "session first"
            );
        }

        List<ConversationMessage> history = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(session.getId());

        if (history.isEmpty()) {

            throw new BadRequestException(
                    "Session has no messages to evaluate"
            );
        }

        String transcript = history.stream()
                .map(message -> (
                        message.getRole() == MessageRole.USER
                                ? "Learner: "
                                : "Tutor: "
                ) + message.getContent())
                .collect(Collectors.joining("\n"));

        AiJsonResult<SessionEvaluationAiResult> result = aiJsonHelper
                .requestJson(
                        SYSTEM_PROMPT,
                        transcript,
                        0.3,
                        SessionEvaluationAiResult.class
                );

        if (!result.isSuccess()) {

            return SessionEvaluationResponse.builder()
                    .sessionId(session.getId())
                    .summary(result.getRaw())
                    .strengths("")
                    .areasToImprove("")
                    .estimatedLevel(null)
                    .build();
        }

        SessionEvaluationAiResult parsed = result.getValue();

        return SessionEvaluationResponse.builder()
                .sessionId(session.getId())
                .summary(parsed.getSummary() != null ? parsed.getSummary() : "")
                .strengths(parsed.getStrengths() != null ? parsed.getStrengths() : "")
                .areasToImprove(
                        parsed.getAreasToImprove() != null
                                ? parsed.getAreasToImprove()
                                : ""
                )
                .estimatedLevel(parsed.getEstimatedLevel())
                .build();
    }

    // Internal shape of the AI's JSON reply.
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SessionEvaluationAiResult {

        private String summary;

        private String strengths;

        private String areasToImprove;

        private String estimatedLevel;
    }
}
