package com.example.backend.ai;

// Thrown by every AIProvider implementation on failure, regardless of
// the underlying cause (network error, HTTP error from the provider,
// unexpected/empty response...). GlobalExceptionHandler maps this to
// 502 Bad Gateway - callers never need to know which concrete provider
// is behind the AIProvider they're using.
public class AIProviderException extends RuntimeException {

    public AIProviderException(String message) {
        super(message);
    }

    public AIProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
