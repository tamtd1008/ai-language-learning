package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.SendMessageRequest;
import com.example.backend.dto.request.StartSessionRequest;
import com.example.backend.dto.response.MessageResponse;
import com.example.backend.dto.response.SessionResponse;
import com.example.backend.service.ConversationService;
import com.example.backend.service.ConversationSessionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// SYSTEM-01: conversation session lifecycle, plus AI-01/AI-02/AI-03
// (sending messages within a session and reading its history). Every
// endpoint here acts on behalf of the currently authenticated user
// (Authentication.getName()) - there is no admin override yet, ownership
// is always required.
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ConversationSessionService sessionService;

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ApiResponse<SessionResponse>> startSession(
            Authentication authentication,
            @RequestBody(required = false) StartSessionRequest request
    ) {

        StartSessionRequest body =
                request != null ? request : new StartSessionRequest();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Session started successfully",
                        sessionService.startSession(
                                authentication.getName(),
                                body
                        )
                ));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<ApiResponse<SessionResponse>> endSession(
            Authentication authentication,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Session ended successfully",
                        sessionService.endSession(
                                authentication.getName(),
                                id
                        )
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getMySessions(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        sessionService.getMySessions(
                                authentication.getName()
                        )
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            Authentication authentication,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        sessionService.getSession(
                                authentication.getName(),
                                id
                        )
                )
        );
    }

    // AI-01/AI-02: send a message in this session, get the AI's reply.
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        conversationService.sendMessage(
                                authentication.getName(),
                                id,
                                request
                        )
                ));
    }

    // Full message history for this session, in chronological order.
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            Authentication authentication,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        conversationService.getMessages(
                                authentication.getName(),
                                id
                        )
                )
        );
    }
}
