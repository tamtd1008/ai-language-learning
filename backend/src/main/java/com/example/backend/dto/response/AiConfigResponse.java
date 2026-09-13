package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AiConfigResponse {

    private Long id;

    private String name;

    private String provider;

    private String model;

    private String systemPrompt;

    private Double temperature;

    private Integer maxTokens;

    private boolean active;

    private LocalDateTime createdAt;
}
