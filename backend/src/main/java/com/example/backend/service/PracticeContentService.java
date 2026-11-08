package com.example.backend.service;

import com.example.backend.ai.AIProviderException;
import com.example.backend.ai.AiJsonHelper;
import com.example.backend.dto.request.CheckAnswerRequest;
import com.example.backend.dto.request.CheckListeningAnswerRequest;
import com.example.backend.dto.request.GenerateExerciseRequest;
import com.example.backend.dto.response.CheckAnswerResponse;
import com.example.backend.dto.response.GrammarExerciseResponse;
import com.example.backend.dto.response.ListeningExerciseResponse;
import com.example.backend.dto.response.PronunciationPromptResponse;
import com.example.backend.dto.response.SpeakingPromptResponse;
import com.example.backend.entity.CefrLevel;
import com.example.backend.entity.Topic;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.LearnerLevelRepository;
import com.example.backend.repository.TopicRepository;
import com.example.backend.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.stereotype.Service;

/**
 * LEARN-03 (grammar), LEARN-04 (listening), LEARN-05 (speaking prompts),
 * LEARN-06 (pronunciation prompts). All exercises are generated and
 * graded statelessly - nothing is persisted here (no "Exercise" table):
 * the check endpoints send the AI the exact question/passage text back
 * alongside the learner's answer and let it grade directly, rather than
 * comparing against a stored answer key. Simpler, and avoids ever
 * needing to keep generation and checking in sync.
 *
 * LEARN-05/LEARN-06 only generate prompts - grading reuses existing
 * endpoints (AI-07 /api/ai/analyze-answer, SPEECH /api/speech/assess-
 * pronunciation) rather than duplicating that logic here.
 */
@Service
@RequiredArgsConstructor
public class PracticeContentService {

    private static final String GRAMMAR_GENERATE_PROMPT =
            "You are a grammar exercise generator for a language " +
                    "learning app. Generate ONE grammar practice question " +
                    "appropriate for a learner at the given CEFR level. " +
                    "Vary the question type across calls: fill-in-the-" +
                    "blank, choosing the correct verb form, correcting a " +
                    "sentence, etc. The question text must be fully " +
                    "self-contained (include any multiple-choice options " +
                    "inline) so it can be shown to the learner with no " +
                    "other context. Respond with ONLY a single JSON " +
                    "object - no markdown fences, no commentary - in " +
                    "exactly this shape: {\"question\": \"<the full " +
                    "exercise question text>\"}.";

    private static final String GRAMMAR_CHECK_PROMPT =
            "You are grading a learner's answer to a grammar exercise. " +
                    "Given the exercise question and the learner's " +
                    "answer, decide if it is correct. Respond with ONLY " +
                    "a single JSON object - no markdown fences, no " +
                    "commentary - in exactly this shape: {\"correct\": " +
                    "<true or false>, \"correctAnswer\": \"<the correct " +
                    "answer>\", \"explanation\": \"<short explanation in " +
                    "Vietnamese of why, especially if incorrect>\"}.";

    private static final String LISTENING_GENERATE_PROMPT =
            "You are a listening-comprehension exercise generator for a " +
                    "language learning app. Generate a short passage (2 " +
                    "to 4 simple sentences) appropriate for a learner at " +
                    "the given CEFR level, meant to be read aloud via " +
                    "text-to-speech. Then write ONE comprehension " +
                    "question about the passage. Respond with ONLY a " +
                    "single JSON object - no markdown fences, no " +
                    "commentary - in exactly this shape: {\"passage\": " +
                    "\"<the passage text>\", \"question\": \"<a " +
                    "comprehension question about the passage>\"}.";

    private static final String LISTENING_CHECK_PROMPT =
            "You are grading a learner's answer to a listening " +
                    "comprehension question. You will be given the " +
                    "original passage, the question, and the learner's " +
                    "answer. Respond with ONLY a single JSON object - no " +
                    "markdown fences, no commentary - in exactly this " +
                    "shape: {\"correct\": <true or false>, " +
                    "\"correctAnswer\": \"<the correct answer, based on " +
                    "the passage>\", \"explanation\": \"<short " +
                    "explanation in Vietnamese>\"}.";

    private static final String SPEAKING_PROMPT_GENERATE_PROMPT =
            "You are generating a speaking practice prompt for a " +
                    "language learner. Generate ONE open-ended question " +
                    "or task appropriate for the given CEFR level that " +
                    "encourages the learner to speak for 30 seconds to a " +
                    "minute. Respond with ONLY a single JSON object - no " +
                    "markdown fences, no commentary - in exactly this " +
                    "shape: {\"prompt\": \"<the speaking prompt>\"}.";

    private static final String PRONUNCIATION_PROMPT_GENERATE_PROMPT =
            "You are generating a pronunciation practice sentence for a " +
                    "language learner. Generate ONE sentence appropriate " +
                    "for the given CEFR level - simple and short for " +
                    "beginners, more complex and idiomatic for advanced " +
                    "levels. Prefer sentences with some naturally " +
                    "challenging sounds to pronounce. Respond with ONLY " +
                    "a single JSON object - no markdown fences, no " +
                    "commentary - in exactly this shape: {\"sentence\": " +
                    "\"<the practice sentence>\"}.";

    private final AiJsonHelper aiJsonHelper;

    private final UserRepository userRepository;

    private final LearnerLevelRepository learnerLevelRepository;

    private final TopicRepository topicRepository;

    // ---- LEARN-03: grammar ----

    public GrammarExerciseResponse generateGrammarExercise(
            String username,
            GenerateExerciseRequest request
    ) {

        String userMessage = buildGenerationContext(username, request);

        GrammarAiResult result = requestOrThrow(
                GRAMMAR_GENERATE_PROMPT,
                userMessage,
                GrammarAiResult.class,
                "Could not generate a grammar exercise"
        );

        return GrammarExerciseResponse.builder()
                .question(result.getQuestion())
                .build();
    }

    public CheckAnswerResponse checkGrammarAnswer(CheckAnswerRequest request) {

        String userMessage = "Question: " + request.getQuestion() +
                "\nLearner's answer: " + request.getLearnerAnswer();

        return toCheckResponse(requestOrThrow(
                GRAMMAR_CHECK_PROMPT,
                userMessage,
                CheckAiResult.class,
                "Could not grade this grammar answer"
        ));
    }

    // ---- LEARN-04: listening ----

    public ListeningExerciseResponse generateListeningExercise(
            String username,
            GenerateExerciseRequest request
    ) {

        String userMessage = buildGenerationContext(username, request);

        ListeningAiResult result = requestOrThrow(
                LISTENING_GENERATE_PROMPT,
                userMessage,
                ListeningAiResult.class,
                "Could not generate a listening exercise"
        );

        return ListeningExerciseResponse.builder()
                .passage(result.getPassage())
                .question(result.getQuestion())
                .build();
    }

    public CheckAnswerResponse checkListeningAnswer(
            CheckListeningAnswerRequest request
    ) {

        String userMessage = "Passage: " + request.getPassage() +
                "\nQuestion: " + request.getQuestion() +
                "\nLearner's answer: " + request.getLearnerAnswer();

        return toCheckResponse(requestOrThrow(
                LISTENING_CHECK_PROMPT,
                userMessage,
                CheckAiResult.class,
                "Could not grade this listening answer"
        ));
    }

    // ---- LEARN-05: speaking ----

    public SpeakingPromptResponse generateSpeakingPrompt(
            String username,
            GenerateExerciseRequest request
    ) {

        String userMessage = buildGenerationContext(username, request);

        SpeakingAiResult result = requestOrThrow(
                SPEAKING_PROMPT_GENERATE_PROMPT,
                userMessage,
                SpeakingAiResult.class,
                "Could not generate a speaking prompt"
        );

        return SpeakingPromptResponse.builder()
                .prompt(result.getPrompt())
                .build();
    }

    // ---- LEARN-06: pronunciation ----

    public PronunciationPromptResponse generatePronunciationPrompt(
            String username,
            GenerateExerciseRequest request
    ) {

        String userMessage = buildGenerationContext(username, request);

        PronunciationAiResult result = requestOrThrow(
                PRONUNCIATION_PROMPT_GENERATE_PROMPT,
                userMessage,
                PronunciationAiResult.class,
                "Could not generate a pronunciation practice sentence"
        );

        return PronunciationPromptResponse.builder()
                .sentence(result.getSentence())
                .build();
    }

    // ---- shared helpers ----

    // Builds "CEFR level: X" (+ topic context, if any) for a generation
    // prompt - level falls back to the learner's saved level (LEARN-01),
    // then A2 if that isn't set either.
    private String buildGenerationContext(
            String username,
            GenerateExerciseRequest request
    ) {

        CefrLevel level = resolveLevel(username, request.getLevel());

        StringBuilder context = new StringBuilder("CEFR level: " + level.name());

        if (request.getTopicId() != null) {

            Topic topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Topic not found")
                    );

            context.append("\nTopic (use naturally if it fits): ")
                    .append(topic.getName());
        }

        return context.toString();
    }

    private CefrLevel resolveLevel(String username, String requestedLevel) {

        CefrLevel explicit = CefrLevel.fromString(requestedLevel);

        if (explicit != null) {
            return explicit;
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        return learnerLevelRepository.findByUserId(user.getId())
                .map(learnerLevel -> learnerLevel.getLevel())
                .orElse(CefrLevel.A2);
    }

    private <T> T requestOrThrow(
            String systemPrompt,
            String userMessage,
            Class<T> resultType,
            String failureMessage
    ) {

        T result;
        try {
            result = aiJsonHelper.requestJson(
                    systemPrompt,
                    userMessage,
                    0.7,
                    resultType
            );
        } catch (AIProviderException e) {
            throw new AIProviderException(failureMessage, e);
        }

        return result;
    }

    private CheckAnswerResponse toCheckResponse(CheckAiResult result) {

        return CheckAnswerResponse.builder()
                .correct(Boolean.TRUE.equals(result.getCorrect()))
                .correctAnswer(result.getCorrectAnswer())
                .explanation(result.getExplanation())
                .build();
    }

    // ---- internal AI response shapes ----

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GrammarAiResult {
        private String question;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ListeningAiResult {
        private String passage;
        private String question;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SpeakingAiResult {
        private String prompt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class PronunciationAiResult {
        private String sentence;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CheckAiResult {
        private Boolean correct;
        private String correctAnswer;
        private String explanation;
    }
}
