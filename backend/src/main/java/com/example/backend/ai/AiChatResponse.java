package com.example.backend.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AiChatResponse {

    private String content;

    // Actual model that served the request - useful for logging/PROGRESS
    // even when the caller didn't request a specific one.
    private String model;

    // Nullable - not every provider reports token usage (the mock never
    // does; Ollama and Gemini both do).
    private Integer promptTokens;

    private Integer completionTokens;
}
