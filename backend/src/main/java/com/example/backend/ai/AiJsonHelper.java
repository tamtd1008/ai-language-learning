package com.example.backend.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Shared "ask the model for a JSON object matching this Java type"
 * pattern, used by every AI-0x feature that needs structured output
 * (grammar check, expression suggestions, answer analysis, session
 * evaluation...). Centralizes the markdown-fence stripping and the
 * graceful fallback when a model (especially a small local one) doesn't
 * comply with the "JSON only" instruction, so each feature service only
 * has to define its own prompt and result shape.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiJsonHelper {

    private final AIProvider aiProvider;

    private final ObjectMapper objectMapper;

    public <T> AiJsonResult<T> requestJson(
            String systemPrompt,
            String userMessage,
            Double temperature,
            Class<T> resultType
    ) {

        AiChatResponse response = aiProvider.chat(
                AiChatRequest.builder()
                        .systemPrompt(systemPrompt)
                        .messages(List.of(
                                AiChatMessage.builder()
                                        .role(AiChatRole.USER)
                                        .content(userMessage)
                                        .build()
                        ))
                        .temperature(temperature)
                        .build()
        );

        String cleaned = stripMarkdownFence(response.getContent());

        try {

            T parsed = objectMapper.readValue(cleaned, resultType);

            return new AiJsonResult<>(parsed, cleaned, true);

        } catch (JacksonException e) {

            log.warn(
                    "AI response was not valid JSON for expected type " +
                            "{}, caller will fall back. Raw response: {}",
                    resultType.getSimpleName(),
                    response.getContent()
            );

            return new AiJsonResult<>(null, cleaned, false);
        }
    }

    private String stripMarkdownFence(String content) {

        String trimmed = content.trim();

        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }

        return trimmed.trim();
    }

    @Getter
    public static class AiJsonResult<T> {

        private final T value;

        // Raw (fence-stripped) text from the model - kept even on
        // success so callers can fall back to showing it verbatim if
        // parsing partially succeeded but a field came back null.
        private final String raw;

        private final boolean success;

        public AiJsonResult(T value, String raw, boolean success) {
            this.value = value;
            this.raw = raw;
            this.success = success;
        }
    }
}
