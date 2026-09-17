package com.example.backend.speech;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TranscriptionResult {

    private String text;

    private String languageCode;
}
