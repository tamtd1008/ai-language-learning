package com.example.backend.service;

import com.example.backend.ai.AIProvider;
import com.example.backend.ai.AiChatMessage;
import com.example.backend.ai.AiChatRequest;
import com.example.backend.ai.AiChatResponse;
import com.example.backend.ai.AiChatRole;
import com.example.backend.dto.response.PronunciationAssessmentResponse;
import com.example.backend.dto.response.WordAssessmentResponse;
import com.example.backend.speech.PronunciationAssessmentProvider;
import com.example.backend.speech.PronunciationAssessmentResult;
import com.example.backend.speech.WordAssessment;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SPEECH-01 (overall assessment), SPEECH-02 (scoring), SPEECH-03
 * (fluency), SPEECH-04 (per-word error detection) all come from one
 * PronunciationAssessmentProvider call. SPEECH-05 (improvement
 * guidance) is layered on top here using AIProvider - turns Azure's raw
 * scores/error list into a short, specific, encouraging note, reusing
 * the same AI infrastructure as the rest of the AI module rather than
 * hardcoding template feedback strings.
 */
@Service
@RequiredArgsConstructor
public class PronunciationAssessmentService {

    private static final String FEEDBACK_SYSTEM_PROMPT =
            "You are a supportive pronunciation coach for a language " +
                    "learner. You will be given the sentence the learner " +
                    "was supposed to say, their pronunciation scores " +
                    "(0-100 scale: accuracy, fluency, completeness, " +
                    "overall), and a list of any words that had " +
                    "pronunciation errors with the error type " +
                    "(Mispronunciation, Omission, or Insertion). Write 2 " +
                    "to 3 short sentences of specific, encouraging " +
                    "feedback in Vietnamese. If specific words were " +
                    "flagged, mention them and how to fix them. If " +
                    "scores are already high and no words were flagged, " +
                    "be positive and give one small tip to sound even " +
                    "more natural. Do not use JSON or any special " +
                    "formatting - plain sentences only.";

    private final PronunciationAssessmentProvider pronunciationAssessmentProvider;

    private final AIProvider aiProvider;

    public PronunciationAssessmentResponse assess(
            byte[] audioBytes,
            String referenceText,
            String languageCode
    ) {

        PronunciationAssessmentResult result = pronunciationAssessmentProvider
                .assess(audioBytes, referenceText, languageCode);

        String feedback = generateFeedback(referenceText, result);

        List<WordAssessmentResponse> words = result.getWords() != null
                ? result.getWords().stream()
                        .map(this::toWordResponse)
                        .toList()
                : List.of();

        return PronunciationAssessmentResponse.builder()
                .recognizedText(result.getRecognizedText())
                .accuracyScore(result.getAccuracyScore())
                .fluencyScore(result.getFluencyScore())
                .completenessScore(result.getCompletenessScore())
                .pronScore(result.getPronScore())
                .words(words)
                .feedback(feedback)
                .build();
    }

    private String generateFeedback(
            String referenceText,
            PronunciationAssessmentResult result
    ) {

        String problemWords = result.getWords() == null
                ? ""
                : result.getWords().stream()
                        .filter(word ->
                                word.getErrorType() != null
                                        && !"None".equalsIgnoreCase(word.getErrorType())
                        )
                        .map(word -> word.getWord() + " (" + word.getErrorType() + ")")
                        .collect(Collectors.joining(", "));

        String userMessage = "Reference sentence: " + referenceText +
                "\nOverall pronunciation score: " + result.getPronScore() +
                "\nAccuracy: " + result.getAccuracyScore() +
                ", Fluency: " + result.getFluencyScore() +
                ", Completeness: " + result.getCompletenessScore() +
                (problemWords.isEmpty()
                        ? "\nNo specific word errors were detected."
                        : "\nWords with errors: " + problemWords);

        AiChatResponse response = aiProvider.chat(
                AiChatRequest.builder()
                        .systemPrompt(FEEDBACK_SYSTEM_PROMPT)
                        .messages(List.of(
                                AiChatMessage.builder()
                                        .role(AiChatRole.USER)
                                        .content(userMessage)
                                        .build()
                        ))
                        .temperature(0.5)
                        .build()
        );

        return response.getContent();
    }

    private WordAssessmentResponse toWordResponse(WordAssessment word) {

        return WordAssessmentResponse.builder()
                .word(word.getWord())
                .accuracyScore(word.getAccuracyScore())
                .errorType(word.getErrorType())
                .build();
    }
}
