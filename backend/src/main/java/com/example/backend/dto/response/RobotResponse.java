package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class RobotResponse {

    private Long id;

    private String deviceId;

    private String status;

    private Long currentSessionId;

    private LocalDateTime lastSeenAt;

    private LocalDateTime registeredAt;
}
