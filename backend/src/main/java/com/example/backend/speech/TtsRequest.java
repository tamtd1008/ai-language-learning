package com.example.backend.speech;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TtsRequest {

    private String text;

    // BCP-47 tag (e.g. "en-US"). Nullable - provider default used if null.
    private String languageCode;

    // Provider-specific voice identifier (e.g. Azure's
    // "en-US-JennyNeural"). Nullable - provider default used if null.
    private String voiceName;
}
