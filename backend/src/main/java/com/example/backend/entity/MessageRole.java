package com.example.backend.entity;

// Deliberately separate from com.example.backend.ai.AiChatRole - entities
// shouldn't depend on the AI provider abstraction layer. ConversationService
// maps between the two.
public enum MessageRole {
    USER,
    ASSISTANT
}
