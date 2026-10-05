package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRobotRequest {

    // Stable hardware identifier the robot itself supplies (serial
    // number, config value, etc.) - also used as the WebSocket channel
    // key (ROBOT-06), so it must stay constant across restarts.
    @NotBlank(message = "Device ID is required")
    @Size(max = 100, message = "Device ID must be at most 100 characters")
    private String deviceId;
}
