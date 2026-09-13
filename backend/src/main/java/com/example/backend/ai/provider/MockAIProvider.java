package com.example.backend.ai.provider;

import com.example.backend.ai.AIProvider;
import com.example.backend.ai.AiChatMessage;
import com.example.backend.ai.AiChatRequest;
import com.example.backend.ai.AiChatResponse;
import com.example.backend.ai.AiChatRole;

import org.springframework.stereotype.Component;

/**
 * Deterministic, network-free provider for tests and local development
 * without Ollama running. Echoes the last user message back with a
 * fixed prefix so tests can assert on exact output.
 */
@Component
public class MockAIProvider implements AIProvider {

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {

        String lastUserMessage = "";

        if (request.getMessages() != null) {

            for (AiChatMessage message : request.getMessages()) {
                if (message.getRole() == AiChatRole.USER) {
                    lastUserMessage = message.getContent();
                }
            }
        }

        return AiChatResponse.builder()
                .content("[MOCK] Bạn vừa nói: \"" + lastUserMessage + "\"")
                .model("mock-echo")
                .promptTokens(0)
                .completionTokens(0)
                .build();
    }
}
