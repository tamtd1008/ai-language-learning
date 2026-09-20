package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.UpdateVocabularyProgressRequest;
import com.example.backend.dto.response.ProgressSummaryResponse;
import com.example.backend.dto.response.VocabularyProgressResponse;
import com.example.backend.service.ProgressService;
import com.example.backend.service.VocabularyProgressService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final VocabularyProgressService vocabularyProgressService;

    private final ProgressService progressService;

    // PROGRESS-04: mark a word as LEARNING or LEARNED.
    @PutMapping("/vocabulary/{vocabularyId}")
    public ResponseEntity<ApiResponse<VocabularyProgressResponse>> markVocabularyProgress(
            Authentication authentication,
            @PathVariable Long vocabularyId,
            @Valid @RequestBody UpdateVocabularyProgressRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Vocabulary progress updated successfully",
                        vocabularyProgressService.markProgress(
                                authentication.getName(),
                                vocabularyId,
                                request
                        )
                )
        );
    }

    // PROGRESS-04: list words the learner is tracking, optionally
    // filtered by status, e.g. /api/progress/vocabulary?status=LEARNED
    @GetMapping("/vocabulary")
    public ResponseEntity<ApiResponse<List<VocabularyProgressResponse>>> getMyVocabularyProgress(
            Authentication authentication,
            @RequestParam(required = false) String status
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        vocabularyProgressService.getMyProgress(
                                authentication.getName(),
                                status
                        )
                )
        );
    }

    @DeleteMapping("/vocabulary/{vocabularyId}")
    public ResponseEntity<ApiResponse<Void>> removeVocabularyProgress(
            Authentication authentication,
            @PathVariable Long vocabularyId
    ) {

        vocabularyProgressService.removeProgress(
                authentication.getName(),
                vocabularyId
        );

        return ResponseEntity.ok(
                ApiResponse.success("Vocabulary progress removed successfully")
        );
    }

    // PROGRESS-05/06: aggregate statistics + score trend over time.
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ProgressSummaryResponse>> getSummary(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        progressService.getSummary(authentication.getName())
                )
        );
    }
}
