package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVocabularyProgressRequest {

    // Validated against VocabularyProgressStatus in the service layer
    // (LEARNING or LEARNED), same pattern as CefrLevel elsewhere.
    @NotBlank(message = "Status is required")
    private String status;
}
