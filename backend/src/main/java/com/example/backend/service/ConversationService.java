package com.example.backend.service;

import com.example.backend.ai.AIProvider;
import com.example.backend.ai.AiResponseGuard;
import com.example.backend.ai.AiChatMessage;
import com.example.backend.ai.AiChatRequest;
import com.example.backend.ai.AiChatResponse;
import com.example.backend.ai.AiChatRole;
import com.example.backend.dto.request.SendMessageRequest;
import com.example.backend.dto.response.MessageResponse;
import com.example.backend.entity.ConversationMessage;
import com.example.backend.entity.ConversationSession;
import com.example.backend.entity.MessageRole;
import com.example.backend.entity.SessionStatus;
import com.example.backend.entity.Topic;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ConversationMessageRepository;
import com.example.backend.repository.ConversationSessionRepository;
import com.example.backend.repository.LearnerLevelRepository;
import com.example.backend.speech.TextToSpeechProvider;
import com.example.backend.speech.TtsRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * AI-01 (chat), AI-02 (answering questions - a natural side effect of
 * chat, not a separate code path) and AI-03 (context): every call sends
 * a bounded recent message history to the AIProvider, keeping the
 * conversation coherent without growing the prompt indefinitely.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are Lingua AI, a friendly, encouraging language tutor. " +
                    "If asked your name, say 'I'm Lingua AI.' " +
                    "Keep replies concise (1-2 short sentences), gently " +
                    "point out any grammar mistakes the learner makes, and " +
                    "reply in the language the learner is practicing.";

    private static final int MAX_CONVERSATION_OUTPUT_TOKENS = 96;

    private final ConversationSessionRepository sessionRepository;

    private final ConversationMessageRepository messageRepository;

    private final LearnerLevelRepository learnerLevelRepository;

    private final AIProvider aiProvider;

    private final TextToSpeechProvider textToSpeechProvider;

    // AI-01/AI-02: send a learner message, get the AI's reply back.
    // Both the learner's message and the AI's reply are persisted, in
    // that order, so getMessages() always reflects exactly what was
    // sent to and received from the model.
    public MessageResponse sendMessage(
            String username,
            Long sessionId,
            SendMessageRequest request
    ) {

        ConversationSession session = findOwnedSessionOrThrow(
                username,
                sessionId
        );

        if (session.getStatus() != SessionStatus.ACTIVE) {

            throw new BadRequestException(
                    "Session has already ended"
            );
        }

        saveMessage(session, MessageRole.USER, request.getContent());

        // AI-03: reload the latest messages (including the message just
        // saved above) so every call is stateless from the AIProvider's
        // point of view while keeping prompt size bounded.
        List<ConversationMessage> history = messageRepository
                .findTop8BySessionIdOrderByCreatedAtDesc(session.getId())
                .stream()
                .filter(message -> message.getRole() != MessageRole.ASSISTANT
                        || !AiResponseGuard.containsInternalReasoning(message.getContent()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.reverse(history);
        Integer configuredMaxTokens = session.getAiConfig() != null
                ? session.getAiConfig().getMaxTokens()
                : null;
        int conversationMaxTokens = configuredMaxTokens == null
                ? MAX_CONVERSATION_OUTPUT_TOKENS
                : Math.min(configuredMaxTokens, MAX_CONVERSATION_OUTPUT_TOKENS);

        AiChatResponse aiResponse = aiProvider.chat(
                AiChatRequest.builder()
                        .systemPrompt(buildSystemPrompt(session))
                        .messages(history.stream()
                                .map(this::toAiChatMessage)
                                .toList())
                        .model(
                                session.getAiConfig() != null
                                        ? session.getAiConfig().getModel()
                                        : null
                        )
                        .temperature(
                                session.getAiConfig() != null
                                        ? session.getAiConfig().getTemperature()
                                        : null
                        )
                        .maxTokens(conversationMaxTokens)
                        .build()
        );

        String learnerVisibleReply = AiResponseGuard.learnerVisibleContent(
                aiResponse.getContent()
        );
        ConversationMessage assistantMessage = saveMessage(
                session,
                MessageRole.ASSISTANT,
                learnerVisibleReply
        );

        return toResponse(assistantMessage);
    }

    // Full history for a session - used to render past turns, e.g. when
    // a learner reopens a session they started earlier.
    public List<MessageResponse> getMessages(String username, Long sessionId) {

        ConversationSession session = findOwnedSessionOrThrow(
                username,
                sessionId
        );

        return messageRepository
                .findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // TTS-04: synthesize audio for one already-saved message in this
    // session - typically the AI's reply, so it can be played back.
    // Re-synthesizes on every call rather than caching; fine for now
    // given conversation turns are short, revisit if this gets costly.
    public byte[] getMessageAudio(
            String username,
            Long sessionId,
            Long messageId
    ) {

        ConversationSession session = findOwnedSessionOrThrow(
                username,
                sessionId
        );

        ConversationMessage message = messageRepository.findById(messageId)
                .filter(m -> m.getSession().getId().equals(session.getId()))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Message not found"
                        )
                );

        return textToSpeechProvider.synthesize(
                TtsRequest.builder()
                        .text(message.getContent())
                        .build()
        );
    }

    public String getMessageAudioContentType() {
        return textToSpeechProvider.getAudioContentType();
    }

    // AI-01 context: combines the session's AiConfig prompt (if any),
    // the session's topic (if any), and the learner's current level
    // (AI-04) - so the same conversation reads very differently for an
    // A1 learner versus a C1 learner, using the exact same AiConfig.
    private String buildSystemPrompt(ConversationSession session) {

        StringBuilder prompt = new StringBuilder();

        String configPrompt = session.getAiConfig() != null
                ? session.getAiConfig().getSystemPrompt()
                : null;

        prompt.append(
                configPrompt != null && !configPrompt.isBlank()
                        ? configPrompt
                        : DEFAULT_SYSTEM_PROMPT
        );

        // Apply a final tutor-output rule even when an admin-configured
        // prompt is selected. Some reasoning models may otherwise put
        // internal planning in the learner-visible message.
        prompt.append(" Reply directly to the learner with only the final answer. " +
                "Never reveal or narrate private reasoning, analysis, or planning, " +
                "and do not describe the learner in the third person. For simple " +
                "greetings or direct questions, use 1-2 short sentences, at most " +
                "35 words, and ask no follow-up unless it helps the practice. " +
                "Keep the answer at the learner's level.");

        Topic topic = session.getTopic();

        if (topic != null) {

            prompt.append(" The conversation topic is \"")
                    .append(topic.getName())
                    .append("\"");

            if (topic.getDescription() != null
                    && !topic.getDescription().isBlank()) {

                prompt.append(" - ").append(topic.getDescription());
            }

            prompt.append(". Keep the conversation focused on this topic.");
        }

        // AI-04: adjust vocabulary/sentence complexity to the learner's
        // self-assessed (or later, AI-assessed) CEFR level. Silently
        // skipped if the learner hasn't set a level yet (LEARN-01) -
        // the conversation still works, just without this tailoring.
        learnerLevelRepository
                .findByUserId(session.getUser().getId())
                .ifPresent(learnerLevel -> prompt
                        .append(" The learner's current level is ")
                        .append(learnerLevel.getLevel().name())
                        .append(" (CEFR). Adjust your vocabulary and ")
                        .append("sentence complexity to match this level - ")
                        .append("simple, short sentences for A1/A2; more ")
                        .append("natural and varied phrasing for B1/B2; ")
                        .append("idiomatic and nuanced language for C1/C2.")
                );

        return prompt.toString();
    }

    private AiChatMessage toAiChatMessage(ConversationMessage message) {

        return AiChatMessage.builder()
                .role(
                        message.getRole() == MessageRole.ASSISTANT
                                ? AiChatRole.ASSISTANT
                                : AiChatRole.USER
                )
                .content(message.getContent())
                .build();
    }

    private ConversationMessage saveMessage(
            ConversationSession session,
            MessageRole role,
            String content
    ) {

        return messageRepository.save(
                ConversationMessage.builder()
                        .session(session)
                        .role(role)
                        .content(content)
                        .build()
        );
    }

    private ConversationSession findOwnedSessionOrThrow(
            String username,
            Long sessionId
    ) {

        return sessionRepository
                .findByIdAndUserUsername(sessionId, username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Session not found"
                        )
                );
    }

    private MessageResponse toResponse(ConversationMessage message) {

        String content = message.getContent();
        if (message.getRole() == MessageRole.ASSISTANT
                && AiResponseGuard.containsInternalReasoning(content)) {
            content = AiResponseGuard.SAFE_FALLBACK;
        }

        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(content)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
