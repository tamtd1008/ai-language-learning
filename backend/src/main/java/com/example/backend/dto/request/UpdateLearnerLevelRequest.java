package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLearnerLevelRequest {

    // Validated against CefrLevel in the service layer (A1..C2) so the
    // error message can list the valid values instead of a generic
    // JSON-parsing failure.
    @NotBlank(message = "Level is required")
    private String level;
}
