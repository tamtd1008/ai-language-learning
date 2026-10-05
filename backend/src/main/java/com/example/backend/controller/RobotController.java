package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.RegisterRobotRequest;
import com.example.backend.dto.request.UpdateRobotStatusRequest;
import com.example.backend.dto.response.RobotResponse;
import com.example.backend.service.RobotService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ROBOT-01 (register), ROBOT-07 (check status), ROBOT-08 (control
// status). The realtime channel (ROBOT-06) lives in the WebSocket
// handler at /ws/robots/{deviceId}, not here. A robot authenticates with
// a normal user JWT - it acts on behalf of the account that registered it.
@RestController
@RequestMapping("/api/robots")
@RequiredArgsConstructor
public class RobotController {

    private final RobotService robotService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RobotResponse>> register(
            Authentication authentication,
            @Valid @RequestBody RegisterRobotRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Robot registered successfully",
                        robotService.register(
                                authentication.getName(),
                                request.getDeviceId()
                        )
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RobotResponse>>> listMyRobots(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        robotService.listMyRobots(authentication.getName())
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RobotResponse>> getStatus(
            Authentication authentication,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        robotService.getStatus(authentication.getName(), id)
                )
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RobotResponse>> updateStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRobotStatusRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Robot status updated successfully",
                        robotService.updateStatus(
                                authentication.getName(),
                                id,
                                request
                        )
                ));
    }
}
