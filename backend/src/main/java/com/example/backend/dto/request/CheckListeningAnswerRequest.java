package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckListeningAnswerRequest {

    @NotBlank(message = "Passage is required")
    private String passage;

    @NotBlank(message = "Question is required")
    private String question;

    @NotBlank(message = "Answer is required")
    private String learnerAnswer;
}
