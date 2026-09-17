package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PronunciationAssessmentResponse {

    // What was actually heard - compare against the reference text you sent.
    private String recognizedText;

    private Double accuracyScore;

    private Double fluencyScore;

    private Double completenessScore;

    private Double pronScore;

    private List<WordAssessmentResponse> words;

    // AI-generated improvement guidance in Vietnamese (SPEECH-05).
    private String feedback;
}
