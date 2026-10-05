package com.example.backend.service;

import com.example.backend.dto.request.UpdateRobotStatusRequest;
import com.example.backend.dto.response.RobotResponse;
import com.example.backend.entity.ConversationSession;
import com.example.backend.entity.Robot;
import com.example.backend.entity.RobotStatus;
import com.example.backend.entity.User;
import com.example.backend.exception.DuplicateResourceException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ConversationSessionRepository;
import com.example.backend.repository.RobotRepository;
import com.example.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ROBOT-01 (connect/register), ROBOT-07 (check status), ROBOT-08 (control
 * status). ROBOT-02/03/04/05 (capture audio, upload it, receive the AI's
 * reply, play it back) need no new code here - a robot does those by
 * acting as an ordinary client of the STT-04/TTS-04 endpoints
 * (POST /api/sessions/{id}/voice-messages and GET
 * .../messages/{id}/audio). ROBOT-06 (realtime channel) is the
 * WebSocket handler, which calls the *ByDeviceId methods below.
 */
@Service
@RequiredArgsConstructor
public class RobotService {

    private final RobotRepository robotRepository;

    private final UserRepository userRepository;

    private final ConversationSessionRepository sessionRepository;

    // ROBOT-01: idempotent - a robot that restarts and registers again
    // with the same deviceId just refreshes its record instead of
    // failing. A deviceId already claimed by a *different* account is
    // rejected, since deviceId doubles as the WebSocket channel key.
    public RobotResponse register(String username, String deviceId) {

        User user = findUserOrThrow(username);

        Robot robot = robotRepository.findByDeviceId(deviceId)
                .map(existing -> {

                    if (!existing.getUser().getUsername().equals(username)) {

                        throw new DuplicateResourceException(
                                "Device ID is already registered to " +
                                        "another account"
                        );
                    }

                    return existing;
                })
                .orElseGet(() ->
                        Robot.builder()
                                .deviceId(deviceId)
                                .user(user)
                                .build()
                );

        robot.setStatus(RobotStatus.IDLE);
        robot.setLastSeenAt(LocalDateTime.now());

        return toResponse(robotRepository.save(robot));
    }

    // ROBOT-08: change a robot's status (and which session it's serving).
    public RobotResponse updateStatus(
            String username,
            Long robotId,
            UpdateRobotStatusRequest request
    ) {

        Robot robot = findOwnedRobotOrThrow(username, robotId);

        RobotStatus status = RobotStatus.fromString(request.getStatus());

        ConversationSession session = null;

        if (request.getSessionId() != null) {

            session = sessionRepository
                    .findByIdAndUserUsername(request.getSessionId(), username)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Session not found")
                    );
        }

        robot.setStatus(status);
        robot.setCurrentSession(session);
        robot.setLastSeenAt(LocalDateTime.now());

        return toResponse(robotRepository.save(robot));
    }

    // ROBOT-07: check a robot's current status.
    public RobotResponse getStatus(String username, Long robotId) {

        return toResponse(findOwnedRobotOrThrow(username, robotId));
    }

    public List<RobotResponse> listMyRobots(String username) {

        return robotRepository.findByUserUsername(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ---- used by RobotWebSocketHandler (ROBOT-06) ----
    // No ownership check here: the WebSocket handshake already verified
    // that the caller owns this deviceId before the connection opened.

    public void updateStatusByDeviceId(String deviceId, RobotStatus status) {

        robotRepository.findByDeviceId(deviceId).ifPresent(robot -> {
            robot.setStatus(status);
            robot.setLastSeenAt(LocalDateTime.now());
            robotRepository.save(robot);
        });
    }

    private Robot findOwnedRobotOrThrow(String username, Long robotId) {

        // 404 for both "doesn't exist" and "not yours", same as sessions.
        return robotRepository.findByIdAndUserUsername(robotId, username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Robot not found")
                );
    }

    private User findUserOrThrow(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }

    private RobotResponse toResponse(Robot robot) {

        return RobotResponse.builder()
                .id(robot.getId())
                .deviceId(robot.getDeviceId())
                .status(robot.getStatus().name())
                .currentSessionId(
                        robot.getCurrentSession() != null
                                ? robot.getCurrentSession().getId()
                                : null
                )
                .lastSeenAt(robot.getLastSeenAt())
                .registeredAt(robot.getRegisteredAt())
                .build();
    }
}
