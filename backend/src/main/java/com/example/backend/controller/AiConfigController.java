package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.AiConfigRequest;
import com.example.backend.dto.response.AiConfigResponse;
import com.example.backend.service.AiConfigService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// SYSTEM-03: AI configuration is purely an admin concern (model, prompt,
// temperature...), so this lives under /api/admin/** and is protected by
// the existing ADMIN-only rule in SecurityConfig - no per-method
// @PreAuthorize needed here, unlike TopicController.
@RestController
@RequestMapping("/api/admin/ai-configs")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService aiConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AiConfigResponse>>> getAllConfigs() {

        return ResponseEntity.ok(
                ApiResponse.success(aiConfigService.getAllConfigs())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AiConfigResponse>> getConfig(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(aiConfigService.getConfig(id))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AiConfigResponse>> createConfig(
            @Valid @RequestBody AiConfigRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "AI config created successfully",
                        aiConfigService.createConfig(request)
                ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AiConfigResponse>> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody AiConfigRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "AI config updated successfully",
                        aiConfigService.updateConfig(id, request)
                )
        );
    }

    // Marks this config as the active one used by new conversation
    // sessions (SYSTEM-01).
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AiConfigResponse>> activateConfig(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "AI config activated successfully",
                        aiConfigService.activateConfig(id)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(
            @PathVariable Long id
    ) {

        aiConfigService.deleteConfig(id);

        return ResponseEntity.ok(
                ApiResponse.success("AI config deleted successfully")
        );
    }
}
