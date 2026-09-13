package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    private String level;
}
