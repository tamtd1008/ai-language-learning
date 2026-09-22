package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RecommendationResponse {

    // Short, personalized, Vietnamese - based on the learner's actual
    // progress numbers, not a generic template.
    private String message;

    // Real topics/vocabulary from the database (queried, never
    // AI-generated IDs) - topics not yet used in any session, and
    // vocabulary not yet marked LEARNED, both at the learner's level.
    private List<TopicResponse> recommendedTopics;

    private List<VocabularyResponse> recommendedVocabulary;
}
