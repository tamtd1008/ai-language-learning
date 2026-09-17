package com.example.backend.speech;

/**
 * Neutral contract for pronunciation assessment (SPEECH-01 through
 * SPEECH-04 - overall assessment, scoring, fluency, and per-word error
 * detection all come back in one call). Distinct from plain
 * SpeechToTextProvider because it needs a reference text to compare
 * against and returns structured scores, not just transcribed text.
 */
public interface PronunciationAssessmentProvider {

    String getProviderName();

    // referenceText is what the learner was supposed to say; languageCode
    // is a BCP-47 tag, null falls back to the provider's own default.
    PronunciationAssessmentResult assess(
            byte[] audioBytes,
            String referenceText,
            String languageCode
    );
}
