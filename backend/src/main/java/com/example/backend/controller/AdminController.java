package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.service.AdminService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // AUTH-06: Get all users. Admin access only (enforced in SecurityConfig).
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        adminService.getAllUsers()
                )
        );
    }
}
