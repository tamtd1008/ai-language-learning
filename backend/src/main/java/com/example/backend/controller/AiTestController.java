package com.example.backend.controller;

import com.example.backend.ai.AIProvider;
import com.example.backend.ai.AiChatMessage;
import com.example.backend.ai.AiChatRequest;
import com.example.backend.ai.AiChatResponse;
import com.example.backend.ai.AiChatRole;
import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.AiTestChatRequest;
import com.example.backend.dto.response.AiTestChatResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Manual smoke-test endpoint for the AIProvider wiring (Ollama/Gemini/
 * Mock), so the provider abstraction can be verified end-to-end before
 * AI-01's full conversation flow exists. Admin-only, since this bypasses
 * ConversationSession/AiConfig entirely - not meant for learners.
 */
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final AIProvider aiProvider;

    @PostMapping("/test-chat")
    public ResponseEntity<ApiResponse<AiTestChatResponse>> testChat(
            @Valid @RequestBody AiTestChatRequest request
    ) {

        AiChatResponse response = aiProvider.chat(
                AiChatRequest.builder()
                        .systemPrompt(request.getSystemPrompt())
                        .messages(List.of(
                                AiChatMessage.builder()
                                        .role(AiChatRole.USER)
                                        .content(request.getMessage())
                                        .build()
                        ))
                        .build()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        AiTestChatResponse.builder()
                                .provider(aiProvider.getProviderName())
                                .model(response.getModel())
                                .reply(response.getContent())
                                .build()
                )
        );
    }
}
