package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.VocabularyRequest;
import com.example.backend.dto.response.VocabularyResponse;
import com.example.backend.service.VocabularyService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// LEARN-02: vocabulary management.
@RestController
@RequestMapping("/api/vocabulary")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    // Any authenticated learner can browse active vocabulary, optionally
    // filtered by topic and/or level, e.g. /api/vocabulary?topicId=3&level=A2
    @GetMapping
    public ResponseEntity<ApiResponse<List<VocabularyResponse>>> getVocabulary(
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) String level
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        vocabularyService.getVocabulary(topicId, level)
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VocabularyResponse>> getVocabularyItem(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(vocabularyService.getVocabularyItem(id))
        );
    }

    // Admin-only management below.
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<VocabularyResponse>>> getAllVocabulary() {

        return ResponseEntity.ok(
                ApiResponse.success(vocabularyService.getAllVocabulary())
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VocabularyResponse>> createVocabulary(
            @Valid @RequestBody VocabularyRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Vocabulary created successfully",
                        vocabularyService.createVocabulary(request)
                ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VocabularyResponse>> updateVocabulary(
            @PathVariable Long id,
            @Valid @RequestBody VocabularyRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Vocabulary updated successfully",
                        vocabularyService.updateVocabulary(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateVocabulary(
            @PathVariable Long id
    ) {

        vocabularyService.deactivateVocabulary(id);

        return ResponseEntity.ok(
                ApiResponse.success("Vocabulary deactivated successfully")
        );
    }
}
