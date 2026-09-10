package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.ChangePasswordRequest;
import com.example.backend.dto.request.UpdateProfileRequest;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.service.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // AUTH-04: Get the profile of the currently authenticated user.
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            Authentication authentication
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.getCurrentUser(username)
                )
        );
    }

    // AUTH-04: Update the profile of the currently authenticated user.
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Profile updated successfully",
                        userService.updateProfile(username, request)
                )
        );
    }

    // AUTH-05: Change the password of the currently authenticated user.
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        String username = authentication.getName();

        userService.changePassword(username, request);

        return ResponseEntity.ok(
                ApiResponse.success("Password changed successfully")
        );
    }
}
