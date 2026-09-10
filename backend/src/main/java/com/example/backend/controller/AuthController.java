package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.response.AuthResponse;
import com.example.backend.service.AuthService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // AUTH-01: Handle user registration requests.
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Register successfully"));
    }

    // AUTH-02: Handle user login requests and return a JWT token.
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successfully",
                        authService.login(request)
                )
        );
    }

    // AUTH-03: Logout.
    // Stateless JWT has nothing to invalidate server-side yet; this
    // endpoint exists so the frontend has a consistent call to make.
    // Revisit if/when refresh-token or token-blacklist is added.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {

        return ResponseEntity.ok(
                ApiResponse.success("Logout successfully")
        );
    }
}
