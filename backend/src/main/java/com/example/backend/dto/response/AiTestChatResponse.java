package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AiTestChatResponse {

    private String provider;

    private String model;

    private String reply;
}
