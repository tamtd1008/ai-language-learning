package com.example.backend.service;

import com.example.backend.ai.AIProvider;
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

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI-01 (chat), AI-02 (answering questions - a natural side effect of
 * chat, not a separate code path) and AI-03 (context): every call sends
 * the session's full message history to the AIProvider, so the model
 * always sees the whole conversation, not just the latest message.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are a friendly, encouraging foreign-language conversation " +
                    "tutor. Keep replies concise (2-4 sentences), gently " +
                    "point out any grammar mistakes the learner makes, and " +
                    "reply in the language the learner is practicing.";

    private final ConversationSessionRepository sessionRepository;

    private final ConversationMessageRepository messageRepository;

    private final AIProvider aiProvider;

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

        // AI-03: reload the full history (including the message just
        // saved above) so every call is stateless from the AIProvider's
        // point of view - all context comes from what we send it, not
        // from anything remembered between requests.
        List<ConversationMessage> history = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(session.getId());

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
                        .maxTokens(
                                session.getAiConfig() != null
                                        ? session.getAiConfig().getMaxTokens()
                                        : null
                        )
                        .build()
        );

        ConversationMessage assistantMessage = saveMessage(
                session,
                MessageRole.ASSISTANT,
                aiResponse.getContent()
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

    // AI-01 context: combines the session's AiConfig prompt (if any)
    // with the session's topic (if any), so a session about "Travel"
    // actually steers the model toward that topic instead of a generic
    // conversation.
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

        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
