package com.example.backend.speech.provider;

import com.example.backend.speech.SpeechProviderException;
import com.example.backend.speech.SpeechToTextProvider;
import com.example.backend.speech.TranscriptionResult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.MultipartBodyBuilder;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;

/** Local speech-to-text provider backed by the whisper.cpp HTTP server. */
@Slf4j
@Component
public class WhisperCppSpeechToTextProvider implements SpeechToTextProvider {

    private final RestClient restClient;
    private final String defaultLanguage;

    public WhisperCppSpeechToTextProvider(
            @Value("${app.speech.whisper.base-url}") String baseUrl,
            @Value("${app.speech.whisper.language}") String defaultLanguage,
            @Value("${app.speech.whisper.timeout-seconds}") long timeoutSeconds
    ) {
        this.defaultLanguage = defaultLanguage;
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
        return "whisper";
    }

    @Override
    public TranscriptionResult transcribe(byte[] audioBytes, String languageCode) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new SpeechProviderException("Audio is required for transcription");
        }

        String requestedLanguage = languageCode != null && !languageCode.isBlank()
                ? languageCode
                : defaultLanguage;
        boolean detectLanguage = requestedLanguage == null
                || requestedLanguage.isBlank()
                || "auto".equalsIgnoreCase(requestedLanguage);
        String whisperLanguage = detectLanguage
                ? "auto"
                : requestedLanguage.split("[-_]")[0].toLowerCase(Locale.ROOT);

        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        ByteArrayResource audio = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "audio.wav";
            }
        };
        multipart.part("file", audio)
                .filename("audio.wav")
                .contentType(MediaType.parseMediaType("audio/wav"));
        multipart.part("language", whisperLanguage);
        multipart.part("detect_language", Boolean.toString(detectLanguage));
        multipart.part("response_format", "json");
        multipart.part("temperature", "0.0");

        WhisperResponse response;
        try {
            response = restClient.post()
                    .uri("/inference")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart.build())
                    .retrieve()
                    .body(WhisperResponse.class);
        } catch (RestClientException e) {
            log.error("Failed to call local whisper.cpp STT", e);
            throw new SpeechProviderException(
                    "Could not reach whisper.cpp at the configured local address. " +
                            "Start the local-speech Docker profile and check WHISPER_BASE_URL.",
                    e
            );
        }

        if (response == null || response.getText() == null
                || response.getText().isBlank()) {
            throw new SpeechProviderException("No speech was detected in the audio");
        }

        String resultLanguage = languageCode != null && !languageCode.isBlank()
                ? languageCode
                : normalizeLanguageName(response.getLanguage());
        return TranscriptionResult.builder()
                .text(response.getText().trim())
                .languageCode(resultLanguage)
                .build();
    }

    private String normalizeLanguageName(String languageName) {
        if (languageName == null || languageName.isBlank()) {
            return null;
        }
        for (String isoCode : Locale.getISOLanguages()) {
            Locale candidate = Locale.forLanguageTag(isoCode);
            if (candidate.getDisplayLanguage(Locale.ENGLISH)
                    .equalsIgnoreCase(languageName)) {
                return isoCode;
            }
        }
        return languageName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WhisperResponse {
        @JsonProperty("text")
        private String text;

        @JsonProperty("language")
        private String language;
    }
}
