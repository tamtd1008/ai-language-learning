package com.example.backend.service;

import com.example.backend.ai.AiJsonHelper;
import com.example.backend.ai.AiJsonHelper.AiJsonResult;
import com.example.backend.dto.request.GrammarCheckRequest;
import com.example.backend.dto.response.GrammarCheckResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.stereotype.Service;

/**
 * AI-05: grammar correction, standalone from any conversation session -
 * given a sentence/paragraph, returns a corrected version plus a short
 * explanation.
 */
@Service
@RequiredArgsConstructor
public class GrammarCheckService {

    private static final String SYSTEM_PROMPT =
            "You are a strict grammar-correction engine for a language " +
                    "learning app. Given a sentence or short paragraph " +
                    "written by a learner, respond with ONLY a single " +
                    "JSON object - no markdown code fences, no extra " +
                    "commentary before or after it - in exactly this " +
                    "shape: {\"corrected\": \"<the fully corrected " +
                    "text>\", \"hasErrors\": <true or false>, " +
                    "\"explanation\": \"<short explanation of what was " +
                    "wrong, in Vietnamese, or an empty string if " +
                    "hasErrors is false>\"}. If the text has no grammar " +
                    "errors, set corrected to the original text " +
                    "unchanged and hasErrors to false.";

    private final AiJsonHelper aiJsonHelper;

    public GrammarCheckResponse check(GrammarCheckRequest request) {

        AiJsonResult<GrammarCheckAiResult> result = aiJsonHelper.requestJson(
                SYSTEM_PROMPT,
                request.getText(),
                0.2, // low temperature - extraction task, not creative writing
                GrammarCheckAiResult.class
        );

        if (!result.isSuccess()) {

            // Small local models occasionally ignore the "JSON only"
            // instruction - fall back to showing the raw response as
            // the explanation rather than failing the request.
            return GrammarCheckResponse.builder()
                    .original(request.getText())
                    .corrected(request.getText())
                    .hasErrors(true)
                    .explanation(result.getRaw())
                    .build();
        }

        GrammarCheckAiResult parsed = result.getValue();

        return GrammarCheckResponse.builder()
                .original(request.getText())
                .corrected(
                        parsed.getCorrected() != null
                                ? parsed.getCorrected()
                                : request.getText()
                )
                .hasErrors(Boolean.TRUE.equals(parsed.getHasErrors()))
                .explanation(
                        parsed.getExplanation() != null
                                ? parsed.getExplanation()
                                : ""
                )
                .build();
    }

    // Internal shape of the AI's JSON reply - never exposed outside this
    // service (GrammarCheckResponse is the public DTO).
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GrammarCheckAiResult {

        private String corrected;

        private Boolean hasErrors;

        private String explanation;
    }
}
