package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SessionResponse {

    private Long id;

    private String status;

    private Long topicId;

    private String topicName;

    private String aiConfigName;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
