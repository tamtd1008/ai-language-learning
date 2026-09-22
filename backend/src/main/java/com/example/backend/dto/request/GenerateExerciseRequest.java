package com.example.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateExerciseRequest {

    // Optional - validated against CefrLevel in the service layer.
    // Falls back to the learner's current level (LEARN-01), then A2 if
    // that isn't set either.
    private String level;

    // Optional - gives the AI topical context (e.g. "Travel") to make
    // the exercise about, instead of a generic sentence.
    private Long topicId;
}
