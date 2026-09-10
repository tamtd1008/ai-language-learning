package com.example.backend.exception;

import org.springframework.http.HttpStatus;

// Thrown when a requested entity (user, topic, session, ...) does not exist.
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String message) {

        super(HttpStatus.NOT_FOUND, message);
    }
}
