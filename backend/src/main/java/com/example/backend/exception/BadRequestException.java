package com.example.backend.exception;

import org.springframework.http.HttpStatus;

// Thrown for invalid input/business rule violations that are the
// client's fault but don't fit the other specific exception types.
public class BadRequestException extends AppException {

    public BadRequestException(String message) {

        super(HttpStatus.BAD_REQUEST, message);
    }
}
