package com.example.backend.service;

import com.example.backend.dto.request.AiConfigRequest;
import com.example.backend.dto.response.AiConfigResponse;
import com.example.backend.entity.AiConfig;
import com.example.backend.exception.DuplicateResourceException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AiConfigRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiConfigService {

    private final AiConfigRepository aiConfigRepository;

    public List<AiConfigResponse> getAllConfigs() {

        return aiConfigRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AiConfigResponse getConfig(Long id) {

        return toResponse(findConfigOrThrow(id));
    }

    // Used internally (e.g. by ConversationSessionService) to pick the
    // config new sessions should use. Not exposed as its own endpoint yet.
    public Optional<AiConfig> getActiveConfigEntity() {

        return aiConfigRepository.findByActiveTrue();
    }

    public AiConfigResponse createConfig(AiConfigRequest request) {

        if (aiConfigRepository.existsByName(request.getName())) {

            throw new DuplicateResourceException(
                    "AI config name already exists"
            );
        }

        AiConfig config = AiConfig.builder()
                .name(request.getName())
                .provider(request.getProvider())
                .model(request.getModel())
                .systemPrompt(request.getSystemPrompt())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .active(false)
                .build();

        return toResponse(aiConfigRepository.save(config));
    }

    public AiConfigResponse updateConfig(Long id, AiConfigRequest request) {

        AiConfig config = findConfigOrThrow(id);

        if (!config.getName().equals(request.getName())
                && aiConfigRepository.existsByName(request.getName())) {

            throw new DuplicateResourceException(
                    "AI config name already exists"
            );
        }

        config.setName(request.getName());
        config.setProvider(request.getProvider());
        config.setModel(request.getModel());
        config.setSystemPrompt(request.getSystemPrompt());
        config.setTemperature(request.getTemperature());
        config.setMaxTokens(request.getMaxTokens());

        return toResponse(aiConfigRepository.save(config));
    }

    // Marks this config as the one new sessions should use, and
    // deactivates whichever config was active before it - only one
    // config is ever active at a time.
    @Transactional
    public AiConfigResponse activateConfig(Long id) {

        AiConfig target = findConfigOrThrow(id);

        aiConfigRepository.findByActiveTrue()
                .filter(current -> !current.getId().equals(id))
                .ifPresent(current -> {
                    current.setActive(false);
                    aiConfigRepository.save(current);
                });

        target.setActive(true);

        return toResponse(aiConfigRepository.save(target));
    }

    public void deleteConfig(Long id) {

        AiConfig config = findConfigOrThrow(id);
        aiConfigRepository.delete(config);
    }

    private AiConfig findConfigOrThrow(Long id) {

        return aiConfigRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AI config not found"
                        )
                );
    }

    private AiConfigResponse toResponse(AiConfig config) {

        return AiConfigResponse.builder()
                .id(config.getId())
                .name(config.getName())
                .provider(config.getProvider())
                .model(config.getModel())
                .systemPrompt(config.getSystemPrompt())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .active(config.isActive())
                .createdAt(config.getCreatedAt())
                .build();
    }
}
