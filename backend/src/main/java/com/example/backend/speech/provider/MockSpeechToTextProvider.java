package com.example.backend.speech.provider;

import com.example.backend.speech.SpeechToTextProvider;
import com.example.backend.speech.TranscriptionResult;

import org.springframework.stereotype.Component;

/**
 * Deterministic, network-free STT provider for tests and local
 * development without an Azure subscription key. Doesn't actually
 * transcribe anything - just confirms the audio was received.
 */
@Component
public class MockSpeechToTextProvider implements SpeechToTextProvider {

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public TranscriptionResult transcribe(byte[] audioBytes, String languageCode) {

        int size = audioBytes != null ? audioBytes.length : 0;

        return TranscriptionResult.builder()
                .text("[MOCK] Transcribed " + size + " bytes of audio")
                .languageCode(languageCode != null ? languageCode : "en-US")
                .build();
    }
}
