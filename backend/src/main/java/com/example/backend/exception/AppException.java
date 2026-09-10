package com.example.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all business exceptions in the system.
 * Each subclass carries the HTTP status it should map to,
 * so GlobalExceptionHandler does not need to guess.
 */
@Getter
public abstract class AppException extends RuntimeException {

    private final HttpStatus status;

    protected AppException(HttpStatus status, String message) {

        super(message);
        this.status = status;
    }
}
