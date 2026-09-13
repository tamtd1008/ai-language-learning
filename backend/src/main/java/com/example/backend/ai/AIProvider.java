package com.example.backend.ai;

/**
 * Neutral contract every AI backend (local model, cloud API, or a mock
 * for tests) implements. AI-01+ services depend on this interface only -
 * switching providers is a matter of changing app.ai.provider, never a
 * code change in the services that use it.
 */
public interface AIProvider {

    // Short, stable identifier used to select this provider via the
    // app.ai.provider property (see AIProviderConfig). Must be unique
    // across all AIProvider implementations.
    String getProviderName();

    // Runs one chat completion. Implementations are expected to throw
    // AIProviderException (not a provider-specific exception) on any
    // failure, so callers only ever need to handle one exception type.
    AiChatResponse chat(AiChatRequest request);
}
