package com.example.backend.service;

import com.example.backend.ai.AiJsonHelper;
import com.example.backend.ai.AIProviderException;
import com.example.backend.dto.request.SuggestExpressionRequest;
import com.example.backend.dto.response.ExpressionSuggestion;
import com.example.backend.dto.response.SuggestExpressionResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.stereotype.Service;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * AI-06: given a sentence the learner wrote (or is about to say), suggest
 * a couple of more natural / varied ways to express the same idea -
 * distinct from AI-05, which only fixes what's grammatically wrong.
 * A perfectly correct but stiff/textbook sentence gets suggestions here
 * even though grammar-check would report no errors.
 */
@Service
@RequiredArgsConstructor
public class ExpressionSuggestionService {

    private static final String SYSTEM_PROMPT =
            "You are a helpful language-learning assistant. Given a " +
                    "sentence written by a learner, suggest 2 to 3 " +
                    "alternative, more natural or more varied ways to " +
                    "express the same idea, at a similar or slightly " +
                    "more advanced level. Do not just fix grammar " +
                    "errors - the input may already be grammatically " +
                    "correct but sound stiff or too simple. Respond " +
                    "with ONLY a single JSON object - no markdown code " +
                    "fences, no extra commentary - in exactly this " +
                    "shape: {\"suggestions\": [{\"text\": \"<alternative " +
                    "phrasing>\", \"note\": \"<short Vietnamese note on " +
                    "the nuance or when to use it>\"}]}. Provide 2 to 3 " +
                    "items, never repeat the original sentence unchanged.";

    private final AiJsonHelper aiJsonHelper;

    public SuggestExpressionResponse suggest(SuggestExpressionRequest request) {

        SuggestExpressionAiResult result = aiJsonHelper.requestJson(
                        SYSTEM_PROMPT,
                        request.getText(),
                        0.8, // a bit of creativity is the point here
                        SuggestExpressionAiResult.class
                );

        if (result.getSuggestions() == null
                || result.getSuggestions().size() < 2
                || result.getSuggestions().size() > 3
                || result.getSuggestions().stream().anyMatch(item ->
                item == null || item.getText() == null || item.getText().isBlank()
                        || item.getNote() == null || item.getNote().isBlank())) {
            throw new AIProviderException(
                    "AI returned incomplete expression suggestions. Please retry."
            );
        }

        List<ExpressionSuggestion> suggestions = result
                .getSuggestions()
                .stream()
                .map(item -> ExpressionSuggestion.builder()
                        .text(item.getText())
                        .note(item.getNote())
                        .build())
                .toList();

        return SuggestExpressionResponse.builder()
                .original(request.getText())
                .suggestions(suggestions)
                .build();
    }

    // Internal shape of the AI's JSON reply.
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SuggestExpressionAiResult {

        @Size(min = 2, max = 3)
        private List<SuggestionItemAi> suggestions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SuggestionItemAi {

        private String text;

        private String note;
    }
}
