package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ListeningExerciseResponse {

    // Feed this text into POST /api/speech/synthesize to get playable
    // audio - kept as two separate calls rather than embedding audio
    // bytes in this JSON response.
    private String passage;

    private String question;
}
