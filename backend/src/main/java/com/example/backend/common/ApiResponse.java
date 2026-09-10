package com.example.backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard response wrapper for every REST API in the system.
 * Keeps success/error responses in the same shape so the frontend
 * (and later the robot client) only needs to handle one format.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;

    private String message;

    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Success response with data and a custom message.
    public static <T> ApiResponse<T> success(String message, T data) {

        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // Success response with data only (default message).
    public static <T> ApiResponse<T> success(T data) {

        return success("Success", data);
    }

    // Success response with a message only, no data (e.g. logout).
    public static ApiResponse<Void> success(String message) {

        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .build();
    }

    // Error response with a message only.
    public static ApiResponse<Void> error(String message) {

        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .build();
    }

    // Error response carrying extra data (e.g. field validation errors).
    public static <T> ApiResponse<T> error(String message, T data) {

        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();
    }
}
