package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SynthesizeSpeechRequest {

    @NotBlank(message = "Text is required")
    @Size(max = 2000, message = "Text must be at most 2000 characters")
    private String text;

    // BCP-47 tag (e.g. "en-US", "vi-VN"). Optional - provider default used if omitted.
    private String languageCode;

    // Provider-specific voice id (e.g. Azure's "en-US-JennyNeural"). Optional.
    private String voiceName;
}
