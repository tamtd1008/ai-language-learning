package com.example.backend.exception;

import com.example.backend.ai.AIProviderException;
import com.example.backend.common.ApiResponse;
import com.example.backend.speech.SpeechProviderException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Central place that turns every exception thrown by controllers/services
 * into a consistent ApiResponse body with the right HTTP status.
 * Keep this the ONLY place that maps exception -> HTTP status.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles every custom business exception (404/409/400/401 ...).
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(
            AppException ex
    ) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Handles @Valid failures on request DTOs, returns field -> error message.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Validation failed",
                        fieldErrors
                ));
    }

    // Handles disabled accounts on login (thrown before bad credentials
    // check, since Spring Security checks account status first).
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(
            DisabledException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Account is disabled"));
    }

    // Handles wrong username/password and any other auth failure from
    // Spring Security's AuthenticationManager.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid username or password"));
    }

    // Handles calls to endpoints the authenticated user has no role for
    // (e.g. USER calling /api/admin/**).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You don't have permission to access this resource"));
    }

    // Handles failures from any AI provider (Ollama unreachable, Gemini
    // API error, empty response...) - logs the real cause but tells the
    // client only that the AI service is temporarily unavailable.
    @ExceptionHandler(AIProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleAIProviderException(
            AIProviderException ex
    ) {

        log.error("AI provider error", ex);

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(
                        "AI service is currently unavailable. Please try again later."
                ));
    }

    // Handles failures from any speech provider (Azure unreachable, no
    // speech detected, empty audio...) - same treatment as
    // AIProviderException, just for the STT/TTS side of things.
    @ExceptionHandler(SpeechProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleSpeechProviderException(
            SpeechProviderException ex
    ) {

        log.error("Speech provider error", ex);

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(
                        "Speech service is currently unavailable. Please try again later."
                ));
    }

    // Catch-all safety net. Logs the real exception for debugging but never
    // leaks internal details (stack trace, exception class) to the client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex
    ) {

        log.error("Unexpected error", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
    }
}
