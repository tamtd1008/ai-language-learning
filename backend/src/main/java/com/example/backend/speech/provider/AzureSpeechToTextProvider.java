package com.example.backend.speech.provider;

import com.example.backend.speech.SpeechProviderException;
import com.example.backend.speech.SpeechToTextProvider;
import com.example.backend.speech.TranscriptionResult;

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

/**
 * STT-02/STT-04: speech-to-text via Azure AI Speech's short-audio REST
 * endpoint. Expects WAV, PCM, 16kHz, mono audio - a client (web/robot)
 * needs to record/convert to this format before uploading. No SDK
 * needed, just the subscription key + region.
 */
@Slf4j
@Component
public class AzureSpeechToTextProvider implements SpeechToTextProvider {

    private static final MediaType AUDIO_CONTENT_TYPE =
            MediaType.parseMediaType("audio/wav; codecs=\"audio/pcm\"; samplerate=16000");

    private final RestClient restClient;

    private final String subscriptionKey;

    private final String defaultLanguage;

    public AzureSpeechToTextProvider(
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
    public TranscriptionResult transcribe(byte[] audioBytes, String languageCode) {

        if (subscriptionKey == null || subscriptionKey.isBlank()) {

            throw new SpeechProviderException(
                    "Azure Speech key is not configured. Set AZURE_SPEECH_KEY."
            );
        }

        String language = languageCode != null && !languageCode.isBlank()
                ? languageCode
                : defaultLanguage;

        AzureSttResponse response;

        try {
            response = restClient.post()
                    .uri(
                            "/speech/recognition/conversation/cognitiveservices/v1" +
                                    "?language={language}&format=simple",
                            language
                    )
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .contentType(AUDIO_CONTENT_TYPE)
                    .body(audioBytes)
                    .retrieve()
                    .body(AzureSttResponse.class);

        } catch (RestClientException e) {

            log.error("Failed to call Azure Speech-to-Text", e);

            throw new SpeechProviderException(
                    "Could not reach Azure Speech-to-Text", e
            );
        }

        if (response == null) {

            throw new SpeechProviderException(
                    "Azure Speech-to-Text returned an empty response"
            );
        }

        if ("NoMatch".equalsIgnoreCase(response.getRecognitionStatus())) {

            throw new SpeechProviderException(
                    "No speech was detected in the audio"
            );
        }

        if (!"Success".equalsIgnoreCase(response.getRecognitionStatus())) {

            throw new SpeechProviderException(
                    "Azure Speech-to-Text could not recognize the audio " +
                            "(status: " + response.getRecognitionStatus() + ")"
            );
        }

        return TranscriptionResult.builder()
                .text(response.getDisplayText())
                .languageCode(language)
                .build();
    }

    // ---- Azure's short-audio recognition response shape (format=simple) ----

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class AzureSttResponse {

        @JsonProperty("RecognitionStatus")
        private String recognitionStatus;

        @JsonProperty("DisplayText")
        private String displayText;
    }
}
