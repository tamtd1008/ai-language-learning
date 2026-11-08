package com.example.backend.config;

import com.example.backend.speech.PronunciationAssessmentProvider;
import com.example.backend.speech.SpeechToTextProvider;
import com.example.backend.speech.TextToSpeechProvider;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Picks which SpeechToTextProvider/TextToSpeechProvider/
 * PronunciationAssessmentProvider beans are active, based on a single
 * provider properties, each of which falls back to the legacy
 * app.speech.provider property. STT, TTS, and pronunciation assessment
 * can therefore use different local/cloud implementations.
 */
@Slf4j
@Configuration
public class SpeechProviderConfig {

    @Bean
    @Primary
    public SpeechToTextProvider activeSpeechToTextProvider(
            List<SpeechToTextProvider> providers,
            @Value("${app.speech.stt.provider}") String activeProviderName
    ) {

        SpeechToTextProvider selected = providers.stream()
                .filter(provider ->
                        provider.getProviderName()
                                .equalsIgnoreCase(activeProviderName)
                )
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown app.speech.provider '" + activeProviderName +
                                "'. Available STT providers: " +
                                providers.stream()
                                        .map(SpeechToTextProvider::getProviderName)
                                        .toList()
                ));

        log.info("Active speech-to-text provider: {}", selected.getProviderName());

        return selected;
    }

    @Bean
    @Primary
    public TextToSpeechProvider activeTextToSpeechProvider(
            List<TextToSpeechProvider> providers,
            @Value("${app.speech.tts.provider}") String activeProviderName
    ) {

        TextToSpeechProvider selected = providers.stream()
                .filter(provider ->
                        provider.getProviderName()
                                .equalsIgnoreCase(activeProviderName)
                )
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown app.speech.provider '" + activeProviderName +
                                "'. Available TTS providers: " +
                                providers.stream()
                                        .map(TextToSpeechProvider::getProviderName)
                                        .toList()
                ));

        log.info("Active text-to-speech provider: {}", selected.getProviderName());

        return selected;
    }

    @Bean
    @Primary
    public PronunciationAssessmentProvider activePronunciationAssessmentProvider(
            List<PronunciationAssessmentProvider> providers,
            @Value("${app.speech.assessment.provider}") String activeProviderName
    ) {

        PronunciationAssessmentProvider selected = providers.stream()
                .filter(provider ->
                        provider.getProviderName()
                                .equalsIgnoreCase(activeProviderName)
                )
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown app.speech.provider '" + activeProviderName +
                                "'. Available pronunciation assessment " +
                                "providers: " +
                                providers.stream()
                                        .map(PronunciationAssessmentProvider::getProviderName)
                                        .toList()
                ));

        log.info(
                "Active pronunciation assessment provider: {}",
                selected.getProviderName()
        );

        return selected;
    }
}
