package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.AnalyzeAnswerRequest;
import com.example.backend.dto.response.AnalyzeAnswerResponse;
import com.example.backend.service.AnswerAnalysisService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI-07: standalone answer analysis - given a question and the
// learner's answer, evaluate relevance/grammar/vocabulary.
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AnswerAnalysisController {

    private final AnswerAnalysisService answerAnalysisService;

    @PostMapping("/analyze-answer")
    public ResponseEntity<ApiResponse<AnalyzeAnswerResponse>> analyzeAnswer(
            @Valid @RequestBody AnalyzeAnswerRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        answerAnalysisService.analyze(request)
                )
        );
    }
}
