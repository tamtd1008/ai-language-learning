package com.example.backend.speech.provider;

import com.example.backend.speech.SpeechProviderException;
import com.example.backend.speech.TextToSpeechProvider;
import com.example.backend.speech.TtsRequest;

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
 * TTS-01/02/03/04: text-to-speech via Azure AI Speech's REST endpoint.
 * Returns MP3 audio bytes. Voice (TTS-02) and language (TTS-03) are
 * both request-level overrides - falls back to the configured default
 * voice/language when not given.
 */
@Slf4j
@Component
public class AzureTextToSpeechProvider implements TextToSpeechProvider {

    private final RestClient restClient;

    private final String subscriptionKey;

    private final String defaultLanguage;

    private final String defaultVoice;

    public AzureTextToSpeechProvider(
            @Value("${app.speech.azure.subscription-key}") String subscriptionKey,
            @Value("${app.speech.azure.region}") String region,
            @Value("${app.speech.azure.default-language}") String defaultLanguage,
            @Value("${app.speech.azure.default-voice}") String defaultVoice,
            @Value("${app.speech.azure.timeout-seconds}") long timeoutSeconds
    ) {
        this.subscriptionKey = subscriptionKey;
        this.defaultLanguage = defaultLanguage;
        this.defaultVoice = defaultVoice;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl("https://" + region + ".tts.speech.microsoft.com")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String getProviderName() {
        return "azure";
    }

    @Override
    public byte[] synthesize(TtsRequest request) {

        if (subscriptionKey == null || subscriptionKey.isBlank()) {

            throw new SpeechProviderException(
                    "Azure Speech key is not configured. Set AZURE_SPEECH_KEY."
            );
        }

        String language = request.getLanguageCode() != null
                && !request.getLanguageCode().isBlank()
                ? request.getLanguageCode()
                : defaultLanguage;

        String voice = request.getVoiceName() != null
                && !request.getVoiceName().isBlank()
                ? request.getVoiceName()
                : defaultVoice;

        String ssml = buildSsml(language, voice, request.getText());

        byte[] audio;

        try {
            audio = restClient.post()
                    .uri("/cognitiveservices/v1")
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header(
                            "X-Microsoft-OutputFormat",
                            "audio-16khz-128kbitrate-mono-mp3"
                    )
                    .contentType(MediaType.valueOf("application/ssml+xml"))
                    .body(ssml)
                    .retrieve()
                    .body(byte[].class);

        } catch (RestClientException e) {

            log.error("Failed to call Azure Text-to-Speech", e);

            throw new SpeechProviderException(
                    "Could not reach Azure Text-to-Speech", e
            );
        }

        if (audio == null || audio.length == 0) {

            throw new SpeechProviderException(
                    "Azure Text-to-Speech returned empty audio"
            );
        }

        return audio;
    }

    private String buildSsml(String language, String voice, String text) {

        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");

        return "<speak version='1.0' xml:lang='" + language + "'>" +
                "<voice xml:lang='" + language + "' name='" + voice + "'>" +
                escaped +
                "</voice></speak>";
    }
}
