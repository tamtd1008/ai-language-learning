package com.example.backend.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiResponseGuardTest {

    @Test
    void suppressesTheReasoningLeakSeenInConversation() {
        String leaked = "Okay, the user just asked \"what's your name?\" " +
                "after my previous attempt. Hmm, I see where they're coming from. " +
                "Let me think... I should be honest but friendly.";

        assertTrue(AiResponseGuard.containsInternalReasoning(leaked));
        assertEquals(AiResponseGuard.SAFE_FALLBACK,
                AiResponseGuard.learnerVisibleContent(leaked));
    }

    @Test
    void suppressesReasoningEvenWhenItStartsAfterAHelpfulLookingSentence() {
        String leaked = "Hi! The user just asked what my name is. Let me think.";

        assertTrue(AiResponseGuard.containsInternalReasoning(leaked));
        assertEquals(AiResponseGuard.SAFE_FALLBACK,
                AiResponseGuard.learnerVisibleContent(leaked));
    }

    @Test
    void keepsAnOrdinaryTutorReply() {
        String answer = "Hi! I'm your language tutor. What would you like to practise?";

        assertFalse(AiResponseGuard.containsInternalReasoning(answer));
        assertEquals(answer, AiResponseGuard.learnerVisibleContent(answer));
    }

    @Test
    void treatsBlankResponsesAsUnsafe() {
        assertEquals(AiResponseGuard.SAFE_FALLBACK,
                AiResponseGuard.learnerVisibleContent("  "));
    }
}
