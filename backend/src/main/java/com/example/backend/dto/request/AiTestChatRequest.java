package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiTestChatRequest {

    @NotBlank(message = "Message is required")
    private String message;

    // Optional - lets you test a system prompt without wiring up AiConfig.
    private String systemPrompt;
}
