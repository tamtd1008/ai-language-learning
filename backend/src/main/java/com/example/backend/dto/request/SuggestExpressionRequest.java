package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuggestExpressionRequest {

    @NotBlank(message = "Text is required")
    @Size(max = 1000, message = "Text must be at most 1000 characters")
    private String text;
}
