package com.example.backend.ai.provider;

import com.example.backend.ai.AIProvider;
import com.example.backend.ai.AIProviderException;
import com.example.backend.ai.AiChatMessage;
import com.example.backend.ai.AiChatRequest;
import com.example.backend.ai.AiChatResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default provider for local development: talks to a local Ollama
 * instance (default model: Qwen 3 4B - "qwen3:4b") over Ollama's REST
 * API. No API key needed, but Ollama must be running (`ollama serve`,
 * and `ollama pull qwen3:4b` done at least once).
 */
@Slf4j
@Component
public class OllamaProvider implements AIProvider {

    private final RestClient restClient;

    private final String baseUrl;

    private final String defaultModel;

    public OllamaProvider(
            @Value("${app.ai.ollama.base-url}") String baseUrl,
            @Value("${app.ai.ollama.model}") String defaultModel,
            @Value("${app.ai.ollama.timeout-seconds}") long timeoutSeconds
    ) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;

        // Local inference (especially CPU-only, or the first call after
        // Ollama has to load the model into RAM) can genuinely take a
        // while - default to a generous read timeout rather than the
        // JDK client's default (which can effectively hang forever),
        // so a stuck request fails with a clear 502 instead of the
        // caller just seeing "Timeout was reached" with no explanation.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {

        Map<String, Object> body = buildRequestBody(request);

        OllamaChatResponse response;

        try {
            response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(OllamaChatResponse.class);

        } catch (RestClientException e) {

            log.error("Failed to call Ollama at {}", baseUrl, e);

            throw new AIProviderException(
                    "Could not reach Ollama at " + baseUrl +
                            " (connection failed or the model took too " +
                            "long to respond). Check that Ollama is " +
                            "running and the model is pulled.",
                    e
            );
        }

        if (response == null || response.getMessage() == null) {

            throw new AIProviderException(
                    "Ollama returned an empty response"
            );
        }

        return AiChatResponse.builder()
                .content(stripThinkingTags(response.getMessage().getContent()))
                .model(response.getModel())
                .promptTokens(response.getPromptEvalCount())
                .completionTokens(response.getEvalCount())
                .build();
    }

    // Safety net for models/Ollama versions that inline reasoning into
    // the message content instead of honoring the "think": false
    // request flag. Handles two shapes seen in practice:
    //   1. A well-formed <think>...</think> block.
    //   2. Just a trailing </think> with no opening tag - some chat
    //      templates open the tag themselves as part of the prompt, so
    //      the model's generated text only contains the closing tag;
    //      in that case everything before the LAST </think> is
    //      reasoning and gets discarded.
    private String stripThinkingTags(String content) {

        if (content == null) {
            return null;
        }

        String cleaned = content.replaceAll("(?s)<think>.*?</think>", "");

        int lastClose = cleaned.lastIndexOf("</think>");

        if (lastClose != -1) {
            cleaned = cleaned.substring(lastClose + "</think>".length());
        }

        return cleaned.trim();
    }

    private Map<String, Object> buildRequestBody(AiChatRequest request) {

        List<Map<String, String>> messages = new ArrayList<>();

        if (request.getSystemPrompt() != null
                && !request.getSystemPrompt().isBlank()) {

            messages.add(Map.of(
                    "role", "system",
                    "content", request.getSystemPrompt()
            ));
        }

        if (request.getMessages() != null) {

            for (AiChatMessage message : request.getMessages()) {

                messages.add(Map.of(
                        "role", message.getRole().name().toLowerCase(),
                        "content", message.getContent()
                ));
            }
        }

        Map<String, Object> options = new HashMap<>();

        if (request.getTemperature() != null) {
            options.put("temperature", request.getTemperature());
        }

        if (request.getMaxTokens() != null) {
            options.put("num_predict", request.getMaxTokens());
        }

        Map<String, Object> body = new HashMap<>();
        body.put(
                "model",
                request.getModel() != null ? request.getModel() : defaultModel
        );
        body.put("messages", messages);
        body.put("stream", false);

        // Qwen3 (and other reasoning-capable models) can emit a long
        // internal "thinking" block before the actual answer. Not
        // useful for a language-learning tutor response - the learner
        // should only ever see the final reply. Ollama >= 0.9 honors
        // this flag directly; for older versions / any model that still
        // inlines <think> tags into content, chat() also strips them
        // as a safety net below.
        body.put("think", false);

        if (!options.isEmpty()) {
            body.put("options", options);
        }

        return body;
    }

    // ---- Ollama's /api/chat response shape (only the fields we use) ----

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class OllamaChatResponse {

        private String model;

        private OllamaMessage message;

        @JsonProperty("prompt_eval_count")
        private Integer promptEvalCount;

        @JsonProperty("eval_count")
        private Integer evalCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class OllamaMessage {

        private String role;

        private String content;

        // Present when Ollama separates reasoning into its own field
        // (rather than inlining <think> tags into content). Deliberately
        // unused - never exposed to the learner, same reasoning as the
        // "think": false request flag above.
        private String thinking;
    }
}
