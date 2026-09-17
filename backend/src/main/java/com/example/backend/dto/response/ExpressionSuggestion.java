package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExpressionSuggestion {

    private String text;

    // Vietnamese note on nuance/register/when to use this alternative.
    private String note;
}
