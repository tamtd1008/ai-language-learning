package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MessageResponse {

    private Long id;

    private String role;

    private String content;

    private LocalDateTime createdAt;
}
