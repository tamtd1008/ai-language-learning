package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class VoiceMessageResponse {

    // What the learner's audio was transcribed to (already saved as a
    // USER message - see ConversationService).
    private String transcript;

    // The AI's text reply (already saved as an ASSISTANT message).
    // Fetch its audio separately via GET .../messages/{id}/audio.
    private MessageResponse reply;
}
