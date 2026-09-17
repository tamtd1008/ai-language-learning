package com.example.backend.speech.provider;

import com.example.backend.speech.PronunciationAssessmentProvider;
import com.example.backend.speech.PronunciationAssessmentResult;
import com.example.backend.speech.WordAssessment;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Deterministic, network-free pronunciation assessment for tests and
 * local development without an Azure subscription key. Always returns
 * a fixed "pretty good" score with no errors - only useful for
 * verifying the request/response plumbing, not for actual feedback.
 */
@Component
public class MockPronunciationAssessmentProvider implements PronunciationAssessmentProvider {

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public PronunciationAssessmentResult assess(
            byte[] audioBytes,
            String referenceText,
            String languageCode
    ) {

        List<WordAssessment> words = Arrays.stream(referenceText.trim().split("\\s+"))
                .map(word -> WordAssessment.builder()
                        .word(word)
                        .accuracyScore(90.0)
                        .errorType("None")
                        .build())
                .toList();

        return PronunciationAssessmentResult.builder()
                .recognizedText(referenceText)
                .accuracyScore(90.0)
                .fluencyScore(90.0)
                .completenessScore(100.0)
                .pronScore(90.0)
                .words(words)
                .build();
    }
}
