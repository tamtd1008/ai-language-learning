package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TopicResponse {

    private Long id;

    private String name;

    private String description;

    private String level;

    private boolean active;

    private LocalDateTime createdAt;
}
