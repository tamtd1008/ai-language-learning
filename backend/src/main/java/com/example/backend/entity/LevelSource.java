package com.example.backend.entity;

// How a learner's current level was determined. Only SELF_ASSESSED is
// produced by the system today (LEARN-01); the other two are reserved
// for once a real placement test / AI scoring exists (SCORE, AI-04+).
public enum LevelSource {
    SELF_ASSESSED,
    PLACEMENT_TEST,
    AI_ASSESSED
}
