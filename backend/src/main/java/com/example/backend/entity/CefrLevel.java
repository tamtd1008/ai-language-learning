package com.example.backend.entity;

import com.example.backend.exception.BadRequestException;

// Standard CEFR proficiency scale, shared across Topic, LearnerLevel and
// Vocabulary so "level" means the same thing everywhere in the system.
public enum CefrLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2;

    // Parses user input (request body strings) into a CefrLevel, with a
    // clear 400 error instead of a raw enum/JSON parsing exception.
    public static CefrLevel fromString(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return CefrLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Level must be one of: A1, A2, B1, B2, C1, C2"
            );
        }
    }
}
