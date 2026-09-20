package com.example.backend.service;

import com.example.backend.dto.response.ProgressSummaryResponse;
import com.example.backend.dto.response.ScoreHistoryItem;
import com.example.backend.entity.MessageRole;
import com.example.backend.entity.User;
import com.example.backend.entity.VocabularyProgressStatus;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ConversationMessageRepository;
import com.example.backend.repository.ConversationSessionRepository;
import com.example.backend.repository.LearnerLevelRepository;
import com.example.backend.repository.ScoreRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VocabularyProgressRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PROGRESS-05 (trend over time) and PROGRESS-06 (overall statistics).
 * PROGRESS-01/02 (saving learning/conversation history) need no code
 * here - every session, message and score is already persisted as it
 * happens (SYSTEM-01, AI-01, SCORE-01..04); this service only reads
 * that data back in aggregate. PROGRESS-03 (score tracking) is the
 * scoreHistory field - GET /api/sessions/{id}/score already covers the
 * single-session view.
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserRepository userRepository;

    private final ConversationSessionRepository sessionRepository;

    private final ConversationMessageRepository messageRepository;

    private final ScoreRepository scoreRepository;

    private final LearnerLevelRepository learnerLevelRepository;

    private final VocabularyProgressRepository vocabularyProgressRepository;

    public ProgressSummaryResponse getSummary(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        Long userId = user.getId();

        long totalSessions = sessionRepository.countByUserId(userId);

        long totalMessagesSent = messageRepository
                .countBySession_User_IdAndRole(userId, MessageRole.USER);

        Double averageOverallScore =
                scoreRepository.findAverageOverallScoreByUserId(userId);

        String currentLevel = learnerLevelRepository.findByUserId(userId)
                .map(level -> level.getLevel().name())
                .orElse(null);

        long vocabularyLearnedCount = vocabularyProgressRepository
                .countByUserIdAndStatus(userId, VocabularyProgressStatus.LEARNED);

        long vocabularyLearningCount = vocabularyProgressRepository
                .countByUserIdAndStatus(userId, VocabularyProgressStatus.LEARNING);

        List<ScoreHistoryItem> scoreHistory = scoreRepository
                .findBySession_User_IdOrderBySession_StartedAtAsc(userId)
                .stream()
                .map(score -> ScoreHistoryItem.builder()
                        .sessionId(score.getSession().getId())
                        .startedAt(score.getSession().getStartedAt())
                        .overallScore(score.getOverallScore())
                        .build())
                .toList();

        return ProgressSummaryResponse.builder()
                .totalSessions(totalSessions)
                .totalMessagesSent(totalMessagesSent)
                .averageOverallScore(averageOverallScore)
                .currentLevel(currentLevel)
                .vocabularyLearnedCount(vocabularyLearnedCount)
                .vocabularyLearningCount(vocabularyLearningCount)
                .scoreHistory(scoreHistory)
                .build();
    }
}
