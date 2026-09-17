package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.SuggestExpressionRequest;
import com.example.backend.dto.response.SuggestExpressionResponse;
import com.example.backend.service.ExpressionSuggestionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI-06: standalone expression suggestions - any authenticated learner
// can ask for alternative phrasings without needing an active session.
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ExpressionSuggestionController {

    private final ExpressionSuggestionService expressionSuggestionService;

    @PostMapping("/suggest-expression")
    public ResponseEntity<ApiResponse<SuggestExpressionResponse>> suggestExpression(
            @Valid @RequestBody SuggestExpressionRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        expressionSuggestionService.suggest(request)
                )
        );
    }
}
