package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.CheckAnswerRequest;
import com.example.backend.dto.request.CheckListeningAnswerRequest;
import com.example.backend.dto.request.GenerateExerciseRequest;
import com.example.backend.dto.response.CheckAnswerResponse;
import com.example.backend.dto.response.GrammarExerciseResponse;
import com.example.backend.dto.response.ListeningExerciseResponse;
import com.example.backend.dto.response.PronunciationPromptResponse;
import com.example.backend.dto.response.RecommendationResponse;
import com.example.backend.dto.response.SpeakingPromptResponse;
import com.example.backend.service.PracticeContentService;
import com.example.backend.service.RecommendationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// LEARN-03 (grammar), LEARN-04 (listening), LEARN-05 (speaking prompts),
// LEARN-06 (pronunciation prompts), LEARN-08 (recommendations).
// LEARN-07 (topic-based conversation) needs no endpoint here - it's
// already covered by POST /api/sessions with a topicId.
@RestController
@RequestMapping("/api/learn")
@RequiredArgsConstructor
public class LearnController {

    private final PracticeContentService practiceContentService;

    private final RecommendationService recommendationService;

    // ---- LEARN-03: grammar ----

    @PostMapping("/grammar/exercise")
    public ResponseEntity<ApiResponse<GrammarExerciseResponse>> generateGrammarExercise(
            Authentication authentication,
            @RequestBody(required = false) GenerateExerciseRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        practiceContentService.generateGrammarExercise(
                                authentication.getName(),
                                request != null ? request : new GenerateExerciseRequest()
                        )
                )
        );
    }

    @PostMapping("/grammar/exercise/check")
    public ResponseEntity<ApiResponse<CheckAnswerResponse>> checkGrammarAnswer(
            @Valid @RequestBody CheckAnswerRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        practiceContentService.checkGrammarAnswer(request)
                )
        );
    }

    // ---- LEARN-04: listening ----

    @PostMapping("/listening/exercise")
    public ResponseEntity<ApiResponse<ListeningExerciseResponse>> generateListeningExercise(
            Authentication authentication,
            @RequestBody(required = false) GenerateExerciseRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        practiceContentService.generateListeningExercise(
                                authentication.getName(),
                                request != null ? request : new GenerateExerciseRequest()
                        )
                )
        );
    }

    @PostMapping("/listening/exercise/check")
    public ResponseEntity<ApiResponse<CheckAnswerResponse>> checkListeningAnswer(
            @Valid @RequestBody CheckListeningAnswerRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        practiceContentService.checkListeningAnswer(request)
                )
        );
    }

    // ---- LEARN-05: speaking ----

    @PostMapping("/speaking/prompt")
    public ResponseEntity<ApiResponse<SpeakingPromptResponse>> generateSpeakingPrompt(
            Authentication authentication,
            @RequestBody(required = false) GenerateExerciseRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        practiceContentService.generateSpeakingPrompt(
                                authentication.getName(),
                                request != null ? request : new GenerateExerciseRequest()
                        )
                )
        );
    }

    // ---- LEARN-06: pronunciation ----

    @PostMapping("/pronunciation/prompt")
    public ResponseEntity<ApiResponse<PronunciationPromptResponse>> generatePronunciationPrompt(
            Authentication authentication,
            @RequestBody(required = false) GenerateExerciseRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        practiceContentService.generatePronunciationPrompt(
                                authentication.getName(),
                                request != null ? request : new GenerateExerciseRequest()
                        )
                )
        );
    }

    // ---- LEARN-08: recommendations ----

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendations(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        recommendationService.getRecommendations(
                                authentication.getName()
                        )
                )
        );
    }
}
