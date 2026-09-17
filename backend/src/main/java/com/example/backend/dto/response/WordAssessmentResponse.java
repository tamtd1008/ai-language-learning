package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WordAssessmentResponse {

    private String word;

    private Double accuracyScore;

    private String errorType;
}
