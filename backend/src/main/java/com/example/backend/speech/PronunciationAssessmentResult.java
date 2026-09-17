package com.example.backend.speech;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PronunciationAssessmentResult {

    // What Azure actually heard - may differ from referenceText if the
    // learner mispronounced/omitted/added words.
    private String recognizedText;

    // All four scores are 0-100.
    private Double accuracyScore;

    private Double fluencyScore;

    private Double completenessScore;

    // Overall pronunciation score (Azure's own weighted combination of
    // the three above).
    private Double pronScore;

    private List<WordAssessment> words;
}
