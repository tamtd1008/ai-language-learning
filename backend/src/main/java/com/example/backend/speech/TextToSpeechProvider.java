package com.example.backend.speech;

/**
 * Neutral contract every text-to-speech backend implements. TTS-01/
 * TTS-04 depend on this interface only - switching providers is a
 * matter of changing app.speech.provider, never a code change in the
 * services that use it (same pattern as AIProvider).
 */
public interface TextToSpeechProvider {

    String getProviderName();

    // Returns raw audio bytes (format is provider-specific - Azure
    // returns MP3 by default here, see AzureTextToSpeechProvider).
    byte[] synthesize(TtsRequest request);
}
