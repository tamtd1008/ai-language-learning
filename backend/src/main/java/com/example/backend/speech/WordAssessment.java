package com.example.backend.speech;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WordAssessment {

    private String word;

    // 0-100
    private Double accuracyScore;

    // "None", "Mispronunciation", "Omission", "Insertion" (Azure's
    // vocabulary - kept as-is rather than mapped to an enum, since Mock
    // and any future provider may not share exactly the same set).
    private String errorType;
}
