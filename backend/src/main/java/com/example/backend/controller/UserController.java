package com.example.backend.controller;

import com.example.backend.dto.request.ChangePasswordRequest;
import com.example.backend.dto.request.UpdateProfileRequest;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Get the profile of the currently authenticated user.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                userService.getCurrentUser(username)
        );
    }

    // Update the profile of the currently authenticated user.
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                userService.updateProfile(
                        username,
                        request
                )
        );
    }

    // Change the password of the currently authenticated user.
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request
    ) {

        String username = authentication.getName();

        userService.changePassword(
                username,
                request
        );

        return ResponseEntity.ok(
                "Password changed successfully"
        );
    }
}
