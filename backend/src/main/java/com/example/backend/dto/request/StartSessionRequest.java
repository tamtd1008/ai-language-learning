package com.example.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartSessionRequest {

    // Optional - a session can start with no fixed topic (free conversation).
    private Long topicId;
}
