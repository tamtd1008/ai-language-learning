package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRobotStatusRequest {

    // Validated against RobotStatus in the service layer.
    @NotBlank(message = "Status is required")
    private String status;

    // Which session this robot is now relaying for. Omit or send null
    // to clear it (e.g. when going back to IDLE with nothing active).
    private Long sessionId;
}
