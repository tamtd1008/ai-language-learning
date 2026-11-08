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
import java.util.LinkedHashMap;
import java.util.Map;

/** Local Piper HTTP provider. Piper returns WAV audio bytes. */
@Slf4j
@Component
public class PiperTextToSpeechProvider implements TextToSpeechProvider {

    private final RestClient restClient;
    private final String defaultVoice;

    public PiperTextToSpeechProvider(
            @Value("${app.speech.piper.base-url}") String baseUrl,
            @Value("${app.speech.piper.voice}") String defaultVoice,
            @Value("${app.speech.piper.timeout-seconds}") long timeoutSeconds
    ) {
        this.defaultVoice = defaultVoice;
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
        return "piper";
    }

    @Override
    public String getAudioContentType() {
        return "audio/wav";
    }

    @Override
    public byte[] synthesize(TtsRequest request) {
        if (request.getText() == null || request.getText().isBlank()) {
            throw new SpeechProviderException("Text is required for Piper TTS");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", request.getText());
        String voice = request.getVoiceName() != null
                && !request.getVoiceName().isBlank()
                ? request.getVoiceName()
                : defaultVoice;
        if (voice != null && !voice.isBlank()) {
            body.put("voice", voice);
        }

        try {
            byte[] audio = restClient.post()
                    .uri("/synthesize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.parseMediaType("audio/wav"))
                    .body(body)
                    .retrieve()
                    .body(byte[].class);

            if (audio == null || audio.length == 0) {
                throw new SpeechProviderException("Piper returned empty audio");
            }
            return audio;
        } catch (RestClientException e) {
            log.error("Failed to call local Piper TTS", e);
            throw new SpeechProviderException(
                    "Could not reach Piper at the configured local address. " +
                            "Start the local-speech Docker profile and check PIPER_BASE_URL.",
                    e
            );
        }
    }
}
