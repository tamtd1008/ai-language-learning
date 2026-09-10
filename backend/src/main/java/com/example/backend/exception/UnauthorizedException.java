package com.example.backend.exception;

import org.springframework.http.HttpStatus;

// Thrown for authentication failures that aren't already covered by
// Spring Security's own exceptions (e.g. wrong current password).
public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {

        super(HttpStatus.UNAUTHORIZED, message);
    }
}
