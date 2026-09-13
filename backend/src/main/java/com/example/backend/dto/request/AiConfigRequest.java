package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiConfigRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Model is required")
    private String model;

    private String systemPrompt;

    @NotNull(message = "Temperature is required")
    private Double temperature;

    @NotNull(message = "Max tokens is required")
    @Positive(message = "Max tokens must be positive")
    private Integer maxTokens;
}
