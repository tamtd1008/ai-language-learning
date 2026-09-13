package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.UpdateLearnerLevelRequest;
import com.example.backend.dto.response.LearnerLevelResponse;
import com.example.backend.service.LearnerLevelService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// LEARN-01: determine / record the learner's proficiency level.
@RestController
@RequestMapping("/api/learner-level")
@RequiredArgsConstructor
public class LearnerLevelController {

    private final LearnerLevelService learnerLevelService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LearnerLevelResponse>> getMyLevel(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        learnerLevelService.getMyLevel(
                                authentication.getName()
                        )
                )
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<LearnerLevelResponse>> setMyLevel(
            Authentication authentication,
            @Valid @RequestBody UpdateLearnerLevelRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Level updated successfully",
                        learnerLevelService.setMyLevel(
                                authentication.getName(),
                                request
                        )
                )
        );
    }
}
