package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 4000, message = "Content must be at most 4000 characters")
    private String content;
}
