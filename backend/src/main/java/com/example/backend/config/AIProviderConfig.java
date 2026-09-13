package com.example.backend.config;

import com.example.backend.ai.AIProvider;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Picks which AIProvider bean is "the" AIProvider the rest of the app
 * autowires, based on app.ai.provider (ollama | mock | gemini). Every
 * concrete provider stays a normal @Component and is still individually
 * injectable by concrete type if ever needed (e.g. an admin endpoint
 * that always tests Gemini specifically) - this bean only decides the
 * @Primary one for everyday use.
 */
@Slf4j
@Configuration
public class AIProviderConfig {

    @Bean
    @Primary
    public AIProvider activeAiProvider(
            List<AIProvider> providers,
            @Value("${app.ai.provider}") String activeProviderName
    ) {

        AIProvider selected = providers.stream()
                .filter(provider ->
                        provider.getProviderName()
                                .equalsIgnoreCase(activeProviderName)
                )
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown app.ai.provider '" + activeProviderName +
                                "'. Available providers: " +
                                providers.stream()
                                        .map(AIProvider::getProviderName)
                                        .toList()
                ));

        log.info("Active AI provider: {}", selected.getProviderName());

        return selected;
    }
}
