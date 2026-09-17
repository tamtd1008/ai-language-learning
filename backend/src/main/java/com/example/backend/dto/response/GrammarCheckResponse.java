package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GrammarCheckResponse {

    private String original;

    private String corrected;

    private boolean hasErrors;

    // Vietnamese explanation of what was wrong, or empty string if
    // hasErrors is false.
    private String explanation;
}
