package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GrammarExerciseResponse {

    // Fully self-contained - includes any multiple-choice options
    // inline, so it can be shown to the learner with no other context.
    private String question;
}
