package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.TopicRequest;
import com.example.backend.dto.response.TopicResponse;
import com.example.backend.service.TopicService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    // SYSTEM-02: any authenticated user can browse active topics to
    // pick one for a conversation session.
    @GetMapping
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getActiveTopics() {

        return ResponseEntity.ok(
                ApiResponse.success(topicService.getActiveTopics())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> getTopic(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(topicService.getTopic(id))
        );
    }

    // Admin-only management below - listing every topic (including
    // inactive ones) and full CRUD.
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getAllTopics() {

        return ResponseEntity.ok(
                ApiResponse.success(topicService.getAllTopics())
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TopicResponse>> createTopic(
            @Valid @RequestBody TopicRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Topic created successfully",
                        topicService.createTopic(request)
                ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TopicResponse>> updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody TopicRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Topic updated successfully",
                        topicService.updateTopic(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateTopic(
            @PathVariable Long id
    ) {

        topicService.deactivateTopic(id);

        return ResponseEntity.ok(
                ApiResponse.success("Topic deactivated successfully")
        );
    }
}
