package com.example.backend.speech.provider;

import com.example.backend.speech.TextToSpeechProvider;
import com.example.backend.speech.TtsRequest;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Deterministic, network-free TTS provider for tests and local
 * development without an Azure subscription key. Returns placeholder
 * bytes, NOT real playable audio - only useful for verifying the
 * request/response plumbing works end to end.
 */
@Component
public class MockTextToSpeechProvider implements TextToSpeechProvider {

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public byte[] synthesize(TtsRequest request) {

        return ("MOCK_AUDIO:" + request.getText())
                .getBytes(StandardCharsets.UTF_8);
    }
}
