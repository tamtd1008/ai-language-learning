package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.SendMessageRequest;
import com.example.backend.dto.request.StartSessionRequest;
import com.example.backend.dto.response.MessageResponse;
import com.example.backend.dto.response.SessionEvaluationResponse;
import com.example.backend.dto.response.SessionResponse;
import com.example.backend.dto.response.VoiceMessageResponse;
import com.example.backend.exception.BadRequestException;
import com.example.backend.service.ConversationService;
import com.example.backend.service.ConversationSessionService;
import com.example.backend.service.SessionEvaluationService;
import com.example.backend.service.VoiceConversationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// SYSTEM-01: conversation session lifecycle, plus AI-01/AI-02/AI-03
// (sending messages within a session and reading its history), AI-08
// (evaluating a session once it's ended), and STT-04/TTS-04 (voice
// turns within a session). Every endpoint here acts on behalf of the
// currently authenticated user (Authentication.getName()) - there is
// no admin override yet, ownership is always required.
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ConversationSessionService sessionService;

    private final ConversationService conversationService;

    private final SessionEvaluationService sessionEvaluationService;

    private final VoiceConversationService voiceConversationService;

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

    // AI-08: qualitative evaluation of a completed session (must be
    // ENDED first via PUT /{id}/end).
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<ApiResponse<SessionEvaluationResponse>> evaluateSession(
            Authentication authentication,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        sessionEvaluationService.evaluate(
                                authentication.getName(),
                                id
                        )
                )
        );
    }

    // STT-04: send a voice message in this session - transcribes the
    // uploaded audio, saves it (STT-03), and returns both the transcript
    // and the AI's text reply. Fetch the reply's audio separately via
    // GET .../messages/{messageId}/audio (TTS-04).
    @PostMapping(
            value = "/{id}/voice-messages",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<VoiceMessageResponse>> sendVoiceMessage(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(required = false) String languageCode
    ) {

        if (audio.isEmpty()) {
            throw new BadRequestException("Audio file is required");
        }

        byte[] audioBytes;

        try {
            audioBytes = audio.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded audio file");
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        voiceConversationService.sendVoiceMessage(
                                authentication.getName(),
                                id,
                                audioBytes,
                                languageCode
                        )
                ));
    }

    // TTS-04: synthesize audio for a specific saved message in this
    // session - typically called to play back the AI's reply.
    @GetMapping("/{id}/messages/{messageId}/audio")
    public ResponseEntity<byte[]> getMessageAudio(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long messageId
    ) {

        byte[] audio = conversationService.getMessageAudio(
                authentication.getName(),
                id,
                messageId
        );

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(audio);
    }
}
