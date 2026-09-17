package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class SuggestExpressionResponse {

    private String original;

    private List<ExpressionSuggestion> suggestions;
}
