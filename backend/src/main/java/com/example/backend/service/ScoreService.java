package com.example.backend.service;

import com.example.backend.ai.AIProviderException;
import com.example.backend.ai.AiJsonHelper;
import com.example.backend.dto.response.ScoreResponse;
import com.example.backend.entity.ConversationMessage;
import com.example.backend.entity.ConversationSession;
import com.example.backend.entity.MessageRole;
import com.example.backend.entity.Score;
import com.example.backend.entity.SessionStatus;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ConversationMessageRepository;
import com.example.backend.repository.ConversationSessionRepository;
import com.example.backend.repository.ScoreRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SCORE-01 (grammar), SCORE-02 (vocabulary), SCORE-03 (conversation)
 * come from one AI call, graded against the session transcript.
 * SCORE-04 (overall) is deliberately NOT asked of the AI - it's the
 * plain average of the other three, computed here, so it's always
 * arithmetically consistent and can't drift from what the three
 * category scores actually say.
 */
@Service
@RequiredArgsConstructor
public class ScoreService {

    private static final String SYSTEM_PROMPT =
            "You are grading a completed language-practice conversation " +
                    "transcript between a learner and an AI tutor. Score " +
                    "the learner's performance in three categories, each " +
                    "on a 0-100 scale: grammarScore (correctness of the " +
                    "grammar used), vocabularyScore (range and " +
                    "appropriateness of vocabulary), conversationScore " +
                    "(fluency, relevance to the topic, and overall " +
                    "engagement). Be a fair, consistent grader - do not " +
                    "default to always giving high scores. Respond with " +
                    "ONLY a single JSON object - no markdown code " +
                    "fences, no extra commentary - in exactly this " +
                    "shape: {\"grammarScore\": <number 0-100>, " +
                    "\"vocabularyScore\": <number 0-100>, " +
                    "\"conversationScore\": <number 0-100>}.";

    private final ConversationSessionRepository sessionRepository;

    private final ConversationMessageRepository messageRepository;

    private final ScoreRepository scoreRepository;

    private final AiJsonHelper aiJsonHelper;

    // Computes (or re-computes) the score for an ENDED session and
    // persists it - a second call for the same session updates the
    // existing row rather than creating a duplicate.
    public ScoreResponse computeScore(String username, Long sessionId) {

        ConversationSession session = sessionRepository
                .findByIdAndUserUsername(sessionId, username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Session not found")
                );

        if (session.getStatus() != SessionStatus.ENDED) {

            throw new BadRequestException(
                    "Only ended sessions can be scored - end the " +
                            "session first"
            );
        }

        List<ConversationMessage> history = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(session.getId());

        if (history.isEmpty()) {

            throw new BadRequestException(
                    "Session has no messages to score"
            );
        }

        String transcript = history.stream()
                .map(message -> (
                        message.getRole() == MessageRole.USER
                                ? "Learner: "
                                : "Tutor: "
                ) + message.getContent())
                .collect(Collectors.joining("\n"));

        ScoreAiResult parsed = aiJsonHelper.requestJson(
                SYSTEM_PROMPT,
                transcript,
                0.2, // grading should be consistent, not creative
                ScoreAiResult.class
        );

        if (!validScore(parsed.getGrammarScore())
                || !validScore(parsed.getVocabularyScore())
                || !validScore(parsed.getConversationScore())) {
            throw new AIProviderException(
                    "Could not compute a score. AI scores must be between 0 and 100."
            );
        }

        double overall = (parsed.getGrammarScore()
                + parsed.getVocabularyScore()
                + parsed.getConversationScore()) / 3.0;

        Score score = scoreRepository.findBySessionId(session.getId())
                .orElseGet(() -> Score.builder().session(session).build());

        score.setGrammarScore(parsed.getGrammarScore());
        score.setVocabularyScore(parsed.getVocabularyScore());
        score.setConversationScore(parsed.getConversationScore());
        score.setOverallScore(round1(overall));

        return toResponse(scoreRepository.save(score));
    }

    public ScoreResponse getScore(String username, Long sessionId) {

        ConversationSession session = sessionRepository
                .findByIdAndUserUsername(sessionId, username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Session not found")
                );

        Score score = scoreRepository.findBySessionId(session.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "This session has not been scored yet"
                        )
                );

        return toResponse(score);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private boolean validScore(Double score) {
        return score != null && Double.isFinite(score) && score >= 0 && score <= 100;
    }

    private ScoreResponse toResponse(Score score) {

        return ScoreResponse.builder()
                .sessionId(score.getSession().getId())
                .grammarScore(score.getGrammarScore())
                .vocabularyScore(score.getVocabularyScore())
                .conversationScore(score.getConversationScore())
                .overallScore(score.getOverallScore())
                .createdAt(score.getCreatedAt())
                .build();
    }

    // Internal shape of the AI's JSON reply - deliberately has no
    // overallScore field, see class javadoc.
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ScoreAiResult {

        private Double grammarScore;

        private Double vocabularyScore;

        private Double conversationScore;
    }
}
