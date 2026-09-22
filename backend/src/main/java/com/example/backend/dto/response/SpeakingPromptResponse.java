package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SpeakingPromptResponse {

    // Record yourself answering this, transcribe via POST
    // /api/speech/transcribe, then grade via POST /api/ai/analyze-answer
    // (question = this prompt, answer = the transcript).
    private String prompt;
}
