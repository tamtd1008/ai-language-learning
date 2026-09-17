package com.example.backend.speech;

/**
 * Neutral contract every speech-to-text backend implements. STT-02/
 * STT-04 depend on this interface only - switching providers is a
 * matter of changing app.speech.provider, never a code change in the
 * services that use it (same pattern as AIProvider).
 */
public interface SpeechToTextProvider {

    String getProviderName();

    // languageCode is a BCP-47 tag (e.g. "en-US", "vi-VN"); null lets
    // the provider fall back to its own configured default.
    TranscriptionResult transcribe(byte[] audioBytes, String languageCode);
}
