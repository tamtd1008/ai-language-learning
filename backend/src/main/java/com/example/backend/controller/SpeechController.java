package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.request.SynthesizeSpeechRequest;
import com.example.backend.dto.response.PronunciationAssessmentResponse;
import com.example.backend.dto.response.TranscriptionResponse;
import com.example.backend.exception.BadRequestException;
import com.example.backend.service.PronunciationAssessmentService;
import com.example.backend.speech.SpeechToTextProvider;
import com.example.backend.speech.TextToSpeechProvider;
import com.example.backend.speech.TranscriptionResult;
import com.example.backend.speech.TtsRequest;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

// STT-02, TTS-01/02/03, and SPEECH-01→05: standalone speech endpoints,
// not tied to a conversation session. STT-04/TTS-04 (voice turns inside
// a session) live in SessionController instead, since they need session
// context.
@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
public class SpeechController {

    private final SpeechToTextProvider speechToTextProvider;

    private final TextToSpeechProvider textToSpeechProvider;

    private final PronunciationAssessmentService pronunciationAssessmentService;

    // STT-02: upload audio (WAV, PCM, 16kHz, mono), get back the
    // transcribed text. languageCode is optional (BCP-47, e.g. "en-US").
    @PostMapping(
            value = "/transcribe",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<TranscriptionResponse>> transcribe(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(required = false) String languageCode
    ) {

        if (audio.isEmpty()) {
            throw new BadRequestException("Audio file is required");
        }

        byte[] audioBytes;

        try {
            audioBytes = audio.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded audio file");
        }

        TranscriptionResult result = speechToTextProvider.transcribe(
                audioBytes,
                languageCode
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        TranscriptionResponse.builder()
                                .text(result.getText())
                                .languageCode(result.getLanguageCode())
                                .build()
                )
        );
    }

    // TTS-01/02/03: text in, audio out. voiceName (TTS-02) and
    // languageCode (TTS-03) are both optional per-request overrides.
    @PostMapping("/synthesize")
    public ResponseEntity<byte[]> synthesize(
            @Valid @RequestBody SynthesizeSpeechRequest request
    ) {

        byte[] audio = textToSpeechProvider.synthesize(
                TtsRequest.builder()
                        .text(request.getText())
                        .languageCode(request.getLanguageCode())
                        .voiceName(request.getVoiceName())
                        .build()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(audio);
    }

    // SPEECH-01→05: upload audio (WAV, PCM, 16kHz, mono) of the learner
    // saying referenceText - get back accuracy/fluency/completeness/
    // overall scores, a per-word error breakdown, and AI-generated
    // improvement guidance.
    @PostMapping(
            value = "/assess-pronunciation",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<PronunciationAssessmentResponse>> assessPronunciation(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("referenceText") String referenceText,
            @RequestParam(required = false) String languageCode
    ) {

        if (audio.isEmpty()) {
            throw new BadRequestException("Audio file is required");
        }

        if (referenceText == null || referenceText.isBlank()) {
            throw new BadRequestException("Reference text is required");
        }

        byte[] audioBytes;

        try {
            audioBytes = audio.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded audio file");
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        pronunciationAssessmentService.assess(
                                audioBytes,
                                referenceText,
                                languageCode
                        )
                )
        );
    }
}
