package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.GrammarCheckRequest;
import com.example.backend.dto.response.GrammarCheckResponse;
import com.example.backend.service.GrammarCheckService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI-05: standalone grammar correction - any authenticated learner can
// check a sentence without needing an active conversation session.
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class GrammarCheckController {

    private final GrammarCheckService grammarCheckService;

    @PostMapping("/grammar-check")
    public ResponseEntity<ApiResponse<GrammarCheckResponse>> checkGrammar(
            @Valid @RequestBody GrammarCheckRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(grammarCheckService.check(request))
        );
    }
}
