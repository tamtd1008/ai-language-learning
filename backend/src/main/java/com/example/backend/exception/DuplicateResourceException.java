package com.example.backend.exception;

import org.springframework.http.HttpStatus;

// Thrown when trying to create a resource that already exists
// (duplicate username, email, ...).
public class DuplicateResourceException extends AppException {

    public DuplicateResourceException(String message) {

        super(HttpStatus.CONFLICT, message);
    }
}
