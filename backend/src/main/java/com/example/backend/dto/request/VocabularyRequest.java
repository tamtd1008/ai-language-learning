package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VocabularyRequest {

    @NotBlank(message = "Word is required")
    @Size(max = 100, message = "Word must be at most 100 characters")
    private String word;

    @NotBlank(message = "Meaning is required")
    @Size(max = 500, message = "Meaning must be at most 500 characters")
    private String meaning;

    @Size(max = 500, message = "Example must be at most 500 characters")
    private String example;

    @Size(max = 100, message = "Pronunciation must be at most 100 characters")
    private String pronunciation;

    // Validated against CefrLevel in the service layer, same as Topic/
    // LearnerLevel - optional, a word doesn't have to be leveled yet.
    private String level;

    // Optional - a word doesn't have to belong to a specific topic.
    private Long topicId;
}
