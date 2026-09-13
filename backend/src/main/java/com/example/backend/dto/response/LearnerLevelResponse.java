package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class LearnerLevelResponse {

    // Null when the learner hasn't set a level yet - this is a normal,
    // expected state for a new account, not an error.
    private String level;

    private String source;

    private LocalDateTime updatedAt;
}
