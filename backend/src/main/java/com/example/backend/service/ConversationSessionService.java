package com.example.backend.service;

import com.example.backend.dto.request.StartSessionRequest;
import com.example.backend.dto.response.SessionResponse;
import com.example.backend.entity.AiConfig;
import com.example.backend.entity.ConversationSession;
import com.example.backend.entity.SessionStatus;
import com.example.backend.entity.Topic;
import com.example.backend.entity.User;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ConversationSessionRepository;
import com.example.backend.repository.TopicRepository;
import com.example.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationSessionService {

    private final ConversationSessionRepository sessionRepository;

    private final UserRepository userRepository;

    private final TopicRepository topicRepository;

    private final AiConfigService aiConfigService;

    // SYSTEM-01: start a new conversation session for the current user.
    public SessionResponse startSession(
            String username,
            StartSessionRequest request
    ) {

        User user = findUserOrThrow(username);

        Topic topic = null;

        if (request.getTopicId() != null) {

            topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Topic not found"
                            )
                    );
        }

        // New sessions pick up whichever AiConfig is currently marked
        // active. If none is configured yet (e.g. fresh install before
        // any AiConfig has been activated), the session simply starts
        // without one - AI-01 will need a fallback for that case.
        AiConfig aiConfig = aiConfigService.getActiveConfigEntity()
                .orElse(null);

        ConversationSession session = ConversationSession.builder()
                .user(user)
                .topic(topic)
                .aiConfig(aiConfig)
                .status(SessionStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .build();

        return toResponse(sessionRepository.save(session));
    }

    // SYSTEM-01: end an active session belonging to the current user.
    public SessionResponse endSession(String username, Long sessionId) {

        ConversationSession session = findOwnedSessionOrThrow(
                username,
                sessionId
        );

        if (session.getStatus() == SessionStatus.ENDED) {

            throw new BadRequestException(
                    "Session has already ended"
            );
        }

        session.setStatus(SessionStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());

        return toResponse(sessionRepository.save(session));
    }

    public List<SessionResponse> getMySessions(String username) {

        User user = findUserOrThrow(username);

        return sessionRepository
                .findByUserIdOrderByStartedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SessionResponse getSession(String username, Long sessionId) {

        return toResponse(findOwnedSessionOrThrow(username, sessionId));
    }

    private ConversationSession findOwnedSessionOrThrow(
            String username,
            Long sessionId
    ) {

        // Deliberately reuse the 404 message instead of a 403 here, so a
        // user probing other people's session ids can't tell the
        // difference between "doesn't exist" and "not yours".
        return sessionRepository
                .findByIdAndUserUsername(sessionId, username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Session not found"
                        )
                );
    }

    private User findUserOrThrow(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private SessionResponse toResponse(ConversationSession session) {

        return SessionResponse.builder()
                .id(session.getId())
                .status(session.getStatus().name())
                .topicId(
                        session.getTopic() != null
                                ? session.getTopic().getId()
                                : null
                )
                .topicName(
                        session.getTopic() != null
                                ? session.getTopic().getName()
                                : null
                )
                .aiConfigName(
                        session.getAiConfig() != null
                                ? session.getAiConfig().getName()
                                : null
                )
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();
    }
}
