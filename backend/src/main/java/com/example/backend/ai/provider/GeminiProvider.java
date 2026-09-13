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
 * Cloud adapter for Google's Gemini API - switch to this when a
 * conversation needs more quality/speed than the local Ollama model can
 * give. Fully implemented, just needs GEMINI_API_KEY set and
 * app.ai.provider=gemini.
 *
 * Note: Google renames/retires Gemini model ids periodically - if the
 * configured model starts returning 404, check the current model list
 * at https://ai.google.dev/gemini-api/docs/models and update
 * app.ai.gemini.model (env var GEMINI_MODEL) accordingly.
 */
@Slf4j
@Component
public class GeminiProvider implements AIProvider {

    private final RestClient restClient;

    private final String apiKey;

    private final String defaultModel;

    public GeminiProvider(
            @Value("${app.ai.gemini.api-key}") String apiKey,
            @Value("${app.ai.gemini.model}") String defaultModel,
            @Value("${app.ai.gemini.base-url}") String baseUrl,
            @Value("${app.ai.gemini.timeout-seconds}") long timeoutSeconds
    ) {
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;

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
        return "gemini";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {

        if (apiKey == null || apiKey.isBlank()) {

            throw new AIProviderException(
                    "Gemini API key is not configured. Set GEMINI_API_KEY."
            );
        }

        String model = request.getModel() != null
                ? request.getModel()
                : defaultModel;

        Map<String, Object> body = buildRequestBody(request);

        GeminiResponse response;

        try {
            response = restClient.post()
                    .uri(
                            "/models/{model}:generateContent?key={key}",
                            model,
                            apiKey
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GeminiResponse.class);

        } catch (RestClientException e) {

            log.error("Failed to call Gemini (model={})", model, e);

            throw new AIProviderException(
                    "Could not reach Gemini API", e
            );
        }

        if (response == null
                || response.getCandidates() == null
                || response.getCandidates().isEmpty()) {

            throw new AIProviderException(
                    "Gemini returned an empty response"
            );
        }

        GeminiContent content = response.getCandidates()
                .get(0)
                .getContent();

        String text = (content != null
                && content.getParts() != null
                && !content.getParts().isEmpty())
                ? content.getParts().get(0).getText()
                : "";

        Integer promptTokens = null;
        Integer completionTokens = null;

        if (response.getUsageMetadata() != null) {
            promptTokens = response.getUsageMetadata().getPromptTokenCount();
            completionTokens = response.getUsageMetadata().getCandidatesTokenCount();
        }

        return AiChatResponse.builder()
                .content(text)
                .model(model)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .build();
    }

    private Map<String, Object> buildRequestBody(AiChatRequest request) {

        Map<String, Object> body = new HashMap<>();

        if (request.getSystemPrompt() != null
                && !request.getSystemPrompt().isBlank()) {

            body.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", request.getSystemPrompt()))
            ));
        }

        List<Map<String, Object>> contents = new ArrayList<>();

        if (request.getMessages() != null) {

            for (AiChatMessage message : request.getMessages()) {

                // Gemini uses "user" / "model" instead of "user" / "assistant".
                String role = message.getRole().name().equalsIgnoreCase("ASSISTANT")
                        ? "model"
                        : "user";

                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", message.getContent()))
                ));
            }
        }

        body.put("contents", contents);

        Map<String, Object> generationConfig = new HashMap<>();

        if (request.getTemperature() != null) {
            generationConfig.put("temperature", request.getTemperature());
        }

        if (request.getMaxTokens() != null) {
            generationConfig.put("maxOutputTokens", request.getMaxTokens());
        }

        if (!generationConfig.isEmpty()) {
            body.put("generationConfig", generationConfig);
        }

        return body;
    }

    // ---- Gemini generateContent response shape (only fields we use) ----

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GeminiResponse {

        private List<GeminiCandidate> candidates;

        @JsonProperty("usageMetadata")
        private GeminiUsage usageMetadata;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GeminiCandidate {

        private GeminiContent content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GeminiContent {

        private List<GeminiPart> parts;

        private String role;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GeminiPart {

        private String text;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GeminiUsage {

        @JsonProperty("promptTokenCount")
        private Integer promptTokenCount;

        @JsonProperty("candidatesTokenCount")
        private Integer candidatesTokenCount;
    }
}
