package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyzeAnswerRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 500, message = "Question must be at most 500 characters")
    private String question;

    @NotBlank(message = "Answer is required")
    @Size(max = 2000, message = "Answer must be at most 2000 characters")
    private String answer;
}
