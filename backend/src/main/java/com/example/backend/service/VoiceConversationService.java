package com.example.backend.service;

import com.example.backend.dto.request.SendMessageRequest;
import com.example.backend.dto.response.MessageResponse;
import com.example.backend.dto.response.VoiceMessageResponse;
import com.example.backend.exception.BadRequestException;
import com.example.backend.speech.SpeechToTextProvider;
import com.example.backend.speech.TranscriptionResult;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

/**
 * STT-04: process a conversation turn by voice. Transcribes the
 * uploaded audio, then hands the text off to ConversationService
 * exactly as if the learner had typed it - which also satisfies STT-03
 * (the recognized text ends up saved as a normal USER message, no
 * separate "raw recognition" table needed).
 */
@Service
@RequiredArgsConstructor
public class VoiceConversationService {

    private final SpeechToTextProvider speechToTextProvider;

    private final ConversationService conversationService;

    public VoiceMessageResponse sendVoiceMessage(
            String username,
            Long sessionId,
            byte[] audioBytes,
            String languageCode
    ) {

        TranscriptionResult transcription = speechToTextProvider.transcribe(
                audioBytes,
                languageCode
        );

        if (transcription.getText() == null
                || transcription.getText().isBlank()) {

            throw new BadRequestException(
                    "Could not detect any speech in the audio"
            );
        }

        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setContent(transcription.getText());

        // Ownership and ACTIVE-session checks happen inside
        // sendMessage() - not duplicated here.
        MessageResponse reply = conversationService.sendMessage(
                username,
                sessionId,
                sendMessageRequest
        );

        return VoiceMessageResponse.builder()
                .transcript(transcription.getText())
                .reply(reply)
                .build();
    }
}
