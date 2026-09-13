package com.example.backend.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    // Kept separate from `messages` (rather than a SYSTEM role message)
    // because providers disagree on how a system prompt is passed -
    // Ollama takes a "system" role message, Gemini takes a dedicated
    // systemInstruction field. Each provider adapts this field to
    // whatever its own API expects.
    private String systemPrompt;

    private List<AiChatMessage> messages;

    // Optional overrides - when null, each provider falls back to its
    // own configured default (see application.properties).
    private String model;

    private Double temperature;

    private Integer maxTokens;
}
