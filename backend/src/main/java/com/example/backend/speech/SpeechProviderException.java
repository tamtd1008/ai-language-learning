package com.example.backend.speech;

// Thrown by every SpeechToTextProvider / TextToSpeechProvider
// implementation on failure (network error, provider HTTP error, no
// speech detected, empty audio returned...). GlobalExceptionHandler maps
// this to 502 Bad Gateway, same treatment as AIProviderException.
public class SpeechProviderException extends RuntimeException {

    public SpeechProviderException(String message) {
        super(message);
    }

    public SpeechProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
