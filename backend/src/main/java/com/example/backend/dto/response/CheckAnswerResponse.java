package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CheckAnswerResponse {

    private boolean correct;

    private String correctAnswer;

    // Vietnamese explanation, especially useful when incorrect.
    private String explanation;
}
