package com.example.backend.service;

import com.example.backend.ai.AIProvider;
import com.example.backend.ai.AiChatMessage;
import com.example.backend.ai.AiChatRequest;
import com.example.backend.ai.AiChatResponse;
import com.example.backend.ai.AiChatRole;
import com.example.backend.dto.response.ProgressSummaryResponse;
import com.example.backend.dto.response.RecommendationResponse;
import com.example.backend.dto.response.TopicResponse;
import com.example.backend.dto.response.VocabularyResponse;
import com.example.backend.entity.CefrLevel;
import com.example.backend.entity.ConversationSession;
import com.example.backend.entity.Topic;
import com.example.backend.entity.User;
import com.example.backend.entity.Vocabulary;
import com.example.backend.entity.VocabularyProgress;
import com.example.backend.entity.VocabularyProgressStatus;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ConversationSessionRepository;
import com.example.backend.repository.TopicRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VocabularyProgressRepository;
import com.example.backend.repository.VocabularyRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LEARN-08: recommends what to study next. Deliberately hybrid -
 * WHICH topics/vocabulary to suggest is decided by plain database
 * queries (never by asking the AI to name real IDs, which it could
 * hallucinate); the AI's only job is writing a short, personalized
 * paragraph explaining the recommendation, using the learner's actual
 * progress numbers as context.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int RECOMMENDATION_LIMIT = 3;

    private static final String MESSAGE_SYSTEM_PROMPT =
            "You are a supportive language-learning coach. You will be " +
                    "given a learner's progress statistics (sessions " +
                    "completed, messages sent, average score, current " +
                    "level, vocabulary learned/learning counts). Write 2 " +
                    "to 3 short, encouraging sentences in Vietnamese " +
                    "summarizing their progress and suggesting what to " +
                    "focus on next. If they have very little activity " +
                    "yet, encourage them to get started rather than " +
                    "commenting on \"progress\". Plain sentences only, " +
                    "no JSON, no special formatting.";

    private final UserRepository userRepository;

    private final ConversationSessionRepository sessionRepository;

    private final TopicRepository topicRepository;

    private final VocabularyRepository vocabularyRepository;

    private final VocabularyProgressRepository vocabularyProgressRepository;

    private final ProgressService progressService;

    private final AIProvider aiProvider;

    public RecommendationResponse getRecommendations(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        // Reuse PROGRESS-05/06's aggregation instead of re-querying the
        // same numbers here.
        ProgressSummaryResponse progress = progressService.getSummary(username);

        CefrLevel level = progress.getCurrentLevel() != null
                ? CefrLevel.valueOf(progress.getCurrentLevel())
                : CefrLevel.A2;

        List<TopicResponse> recommendedTopics =
                findRecommendedTopics(user.getId(), level);

        List<VocabularyResponse> recommendedVocabulary =
                findRecommendedVocabulary(user.getId(), level);

        String message = generateMessage(progress);

        return RecommendationResponse.builder()
                .message(message)
                .recommendedTopics(recommendedTopics)
                .recommendedVocabulary(recommendedVocabulary)
                .build();
    }

    private List<TopicResponse> findRecommendedTopics(
            Long userId,
            CefrLevel level
    ) {

        List<Long> attemptedTopicIds = sessionRepository
                .findByUserIdOrderByStartedAtDesc(userId)
                .stream()
                .map(ConversationSession::getTopic)
                .filter(topic -> topic != null)
                .map(Topic::getId)
                .distinct()
                .toList();

        List<Topic> topics = attemptedTopicIds.isEmpty()
                ? topicRepository.findByActiveTrueAndLevel(
                        level, Limit.of(RECOMMENDATION_LIMIT))
                : topicRepository.findByActiveTrueAndLevelAndIdNotIn(
                        level, attemptedTopicIds, Limit.of(RECOMMENDATION_LIMIT));

        return topics.stream()
                .map(topic -> TopicResponse.builder()
                        .id(topic.getId())
                        .name(topic.getName())
                        .description(topic.getDescription())
                        .level(topic.getLevel() != null ? topic.getLevel().name() : null)
                        .active(topic.isActive())
                        .createdAt(topic.getCreatedAt())
                        .build())
                .toList();
    }

    private List<VocabularyResponse> findRecommendedVocabulary(
            Long userId,
            CefrLevel level
    ) {

        List<Long> learnedVocabularyIds = vocabularyProgressRepository
                .findByUserIdAndStatusOrderByUpdatedAtDesc(
                        userId, VocabularyProgressStatus.LEARNED)
                .stream()
                .map(VocabularyProgress::getVocabulary)
                .map(Vocabulary::getId)
                .toList();

        List<Vocabulary> words = learnedVocabularyIds.isEmpty()
                ? vocabularyRepository.findByActiveTrueAndLevel(
                        level, Limit.of(RECOMMENDATION_LIMIT))
                : vocabularyRepository.findByActiveTrueAndLevelAndIdNotIn(
                        level, learnedVocabularyIds, Limit.of(RECOMMENDATION_LIMIT));

        return words.stream()
                .map(word -> VocabularyResponse.builder()
                        .id(word.getId())
                        .word(word.getWord())
                        .meaning(word.getMeaning())
                        .example(word.getExample())
                        .pronunciation(word.getPronunciation())
                        .level(word.getLevel() != null ? word.getLevel().name() : null)
                        .topicId(word.getTopic() != null ? word.getTopic().getId() : null)
                        .topicName(word.getTopic() != null ? word.getTopic().getName() : null)
                        .active(word.isActive())
                        .createdAt(word.getCreatedAt())
                        .build())
                .toList();
    }

    private String generateMessage(ProgressSummaryResponse progress) {

        String userMessage = "Sessions completed: " + progress.getTotalSessions() +
                "\nMessages sent: " + progress.getTotalMessagesSent() +
                "\nAverage overall score: " +
                (progress.getAverageOverallScore() != null
                        ? progress.getAverageOverallScore()
                        : "no scored sessions yet") +
                "\nCurrent level: " +
                (progress.getCurrentLevel() != null
                        ? progress.getCurrentLevel()
                        : "not set yet") +
                "\nVocabulary learned: " + progress.getVocabularyLearnedCount() +
                "\nVocabulary in progress: " + progress.getVocabularyLearningCount();

        AiChatResponse response = aiProvider.chat(
                AiChatRequest.builder()
                        .systemPrompt(MESSAGE_SYSTEM_PROMPT)
                        .messages(List.of(
                                AiChatMessage.builder()
                                        .role(AiChatRole.USER)
                                        .content(userMessage)
                                        .build()
                        ))
                        .temperature(0.6)
                        .build()
        );

        return response.getContent();
    }
}
