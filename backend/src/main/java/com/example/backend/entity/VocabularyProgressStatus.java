package com.example.backend.entity;

import com.example.backend.exception.BadRequestException;

// Same fromString-with-clear-error pattern as CefrLevel.
public enum VocabularyProgressStatus {
    LEARNING,
    LEARNED;

    public static VocabularyProgressStatus fromString(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return VocabularyProgressStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Status must be one of: LEARNING, LEARNED"
            );
        }
    }
}
