package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PronunciationPromptResponse {

    // Read this aloud, then submit via POST
    // /api/speech/assess-pronunciation with this as referenceText.
    private String sentence;
}
