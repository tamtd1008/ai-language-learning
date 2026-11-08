package com.example.backend.ai;

import java.util.regex.Pattern;

/** Prevents model planning text from being shown or replayed in a later turn. */
public final class AiResponseGuard {

    public static final String SAFE_FALLBACK =
            "I'm your language-learning tutor. What would you like to practise today?";

    private static final Pattern INTERNAL_REASONING = Pattern.compile(
            "(?is)(?:^\\s*(?:(?:okay|alright|well|hmm)[,!.]?\\s*)?" +
                    "(?:the user\\s+(?:(?:just|has just)\\s+)?(?:asked|wants|is asking|said)|" +
                    "let me\\s+(?:think|reason|analyze)|" +
                    "i need to\\s+(?:think|recall|figure out|make sure)|" +
                    "we need to\\s+(?:answer|respond)|the key points to hit|" +
                    "i recall they want|this is a job interview context)" +
                    "|\\b(?:i see where (?:they're|they are) coming from|" +
                    "i should be honest|let me think)\\b|</?think>)"
    );

    private AiResponseGuard() {
    }

    public static boolean containsInternalReasoning(String content) {
        return content != null && INTERNAL_REASONING.matcher(content).find();
    }

    public static String learnerVisibleContent(String content) {
        if (content == null || content.isBlank() || containsInternalReasoning(content)) {
            return SAFE_FALLBACK;
        }
        return content.trim();
    }
}
