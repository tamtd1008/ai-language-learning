package com.example.backend.speech.provider;

import com.example.backend.speech.PronunciationAssessmentProvider;
import com.example.backend.speech.PronunciationAssessmentResult;
import com.example.backend.speech.SpeechProviderException;
import com.example.backend.speech.WordAssessment;

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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * SPEECH-01 through SPEECH-04: pronunciation assessment via Azure AI
 * Speech. Reuses the same short-audio recognition endpoint as
 * AzureSpeechToTextProvider, but adds a base64-encoded JSON config in
 * the "Pronunciation-Assessment" header and asks for format=detailed,
 * which makes Azure return accuracy/fluency/completeness/overall scores
 * plus a per-word breakdown (word + error type: None, Mispronunciation,
 * Omission, or Insertion) instead of just plain text.
 */
@Slf4j
@Component
public class AzurePronunciationAssessmentProvider implements PronunciationAssessmentProvider {

    // Same quoting requirement as AzureSpeechToTextProvider's content
    // type - codecs' value contains a "/", which Spring's MediaType
    // parser only accepts when quoted.
    private static final MediaType AUDIO_CONTENT_TYPE =
            MediaType.parseMediaType("audio/wav; codecs=\"audio/pcm\"; samplerate=16000");

    private final RestClient restClient;

    private final String subscriptionKey;

    private final String defaultLanguage;

    public AzurePronunciationAssessmentProvider(
            @Value("${app.speech.azure.subscription-key}") String subscriptionKey,
            @Value("${app.speech.azure.region}") String region,
            @Value("${app.speech.azure.default-language}") String defaultLanguage,
            @Value("${app.speech.azure.timeout-seconds}") long timeoutSeconds
    ) {
        this.subscriptionKey = subscriptionKey;
        this.defaultLanguage = defaultLanguage;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl("https://" + region + ".stt.speech.microsoft.com")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String getProviderName() {
        return "azure";
    }

    @Override
    public PronunciationAssessmentResult assess(
            byte[] audioBytes,
            String referenceText,
            String languageCode
    ) {

        if (subscriptionKey == null || subscriptionKey.isBlank()) {

            throw new SpeechProviderException(
                    "Azure Speech key is not configured. Set AZURE_SPEECH_KEY."
            );
        }

        String language = languageCode != null && !languageCode.isBlank()
                ? languageCode
                : defaultLanguage;

        String pronunciationHeader = buildPronunciationAssessmentHeader(referenceText);

        AzureAssessmentResponse response;

        try {
            response = restClient.post()
                    .uri(
                            "/speech/recognition/conversation/cognitiveservices/v1" +
                                    "?language={language}&format=detailed",
                            language
                    )
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Pronunciation-Assessment", pronunciationHeader)
                    .contentType(AUDIO_CONTENT_TYPE)
                    .body(audioBytes)
                    .retrieve()
                    .body(AzureAssessmentResponse.class);

        } catch (RestClientException e) {

            log.error("Failed to call Azure Pronunciation Assessment", e);

            throw new SpeechProviderException(
                    "Could not reach Azure Pronunciation Assessment", e
            );
        }

        if (response == null) {

            throw new SpeechProviderException(
                    "Azure Pronunciation Assessment returned an empty response"
            );
        }

        if ("NoMatch".equalsIgnoreCase(response.getRecognitionStatus())) {

            throw new SpeechProviderException(
                    "No speech was detected in the audio"
            );
        }

        if (response.getNBest() == null || response.getNBest().isEmpty()) {

            throw new SpeechProviderException(
                    "Azure Pronunciation Assessment returned no result " +
                            "(status: " + response.getRecognitionStatus() + ")"
            );
        }

        NBestItem best = response.getNBest().get(0);
        PronunciationScores scores = best.getPronunciationAssessment();

        List<WordAssessment> words = best.getWords() != null
                ? best.getWords().stream()
                        .map(this::toWordAssessment)
                        .toList()
                : Collections.emptyList();

        return PronunciationAssessmentResult.builder()
                .recognizedText(response.getDisplayText())
                .accuracyScore(scores != null ? scores.getAccuracyScore() : null)
                .fluencyScore(scores != null ? scores.getFluencyScore() : null)
                .completenessScore(scores != null ? scores.getCompletenessScore() : null)
                .pronScore(scores != null ? scores.getPronScore() : null)
                .words(words)
                .build();
    }

    private WordAssessment toWordAssessment(WordItem item) {

        WordScores scores = item.getPronunciationAssessment();

        return WordAssessment.builder()
                .word(item.getWord())
                .accuracyScore(scores != null ? scores.getAccuracyScore() : null)
                .errorType(scores != null && scores.getErrorType() != null
                        ? scores.getErrorType()
                        : "None")
                .build();
    }

    // Builds the base64-encoded JSON Azure expects in the
    // Pronunciation-Assessment header. GradingSystem/Granularity/
    // Dimension are fixed to sensible defaults for this app rather than
    // exposed as options - keeps the public API simple.
    private String buildPronunciationAssessmentHeader(String referenceText) {

        String escapedReference = referenceText
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");

        String configJson = "{"
                + "\"ReferenceText\":\"" + escapedReference + "\","
                + "\"GradingSystem\":\"HundredMark\","
                + "\"Granularity\":\"Word\","
                + "\"Dimension\":\"Comprehensive\","
                + "\"EnableMiscue\":true"
                + "}";

        return Base64.getEncoder().encodeToString(
                configJson.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ---- Azure's format=detailed + Pronunciation-Assessment response shape ----

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class AzureAssessmentResponse {

        @JsonProperty("RecognitionStatus")
        private String recognitionStatus;

        @JsonProperty("DisplayText")
        private String displayText;

        @JsonProperty("NBest")
        private List<NBestItem> nBest;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class NBestItem {

        @JsonProperty("PronunciationAssessment")
        private PronunciationScores pronunciationAssessment;

        @JsonProperty("Words")
        private List<WordItem> words;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class PronunciationScores {

        @JsonProperty("AccuracyScore")
        private Double accuracyScore;

        @JsonProperty("FluencyScore")
        private Double fluencyScore;

        @JsonProperty("CompletenessScore")
        private Double completenessScore;

        @JsonProperty("PronScore")
        private Double pronScore;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class WordItem {

        @JsonProperty("Word")
        private String word;

        @JsonProperty("PronunciationAssessment")
        private WordScores pronunciationAssessment;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class WordScores {

        @JsonProperty("AccuracyScore")
        private Double accuracyScore;

        @JsonProperty("ErrorType")
        private String errorType;
    }
}
