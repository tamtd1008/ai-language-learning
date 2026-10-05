package com.example.backend.entity;

import com.example.backend.exception.BadRequestException;

// OFFLINE: chưa kết nối / mất kết nối WebSocket.
// IDLE: đã kết nối, đang chờ lệnh.
// LISTENING: đang thu âm người dùng.
// SPEAKING: đang phát audio phản hồi.
// ERROR: robot tự báo lỗi (kẹt động cơ, cảm biến lỗi...).
public enum RobotStatus {
    OFFLINE,
    IDLE,
    LISTENING,
    SPEAKING,
    ERROR;

    // Same fromString-with-clear-error pattern as CefrLevel.
    public static RobotStatus fromString(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return RobotStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Status must be one of: OFFLINE, IDLE, LISTENING, SPEAKING, ERROR"
            );
        }
    }
}
