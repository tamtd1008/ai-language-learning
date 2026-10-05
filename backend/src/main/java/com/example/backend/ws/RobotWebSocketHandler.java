package com.example.backend.ws;

import com.example.backend.entity.RobotStatus;
import com.example.backend.service.RobotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ROBOT-06: realtime channel. Protocol is deliberately minimal (plain
 * text, not JSON) since the robot side is a small Python script, not
 * another REST client:
 *   - Robot -> server: sends its current status as a bare word, e.g.
 *     "LISTENING", "SPEAKING", "IDLE" - anything else is ignored rather
 *     than closing the connection, so a typo doesn't kill the session.
 *   - Server -> robot: sendCommand() below pushes a bare word too, e.g.
 *     "START_LISTENING" - the robot script defines what it does with
 *     each command; the backend doesn't need to know.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RobotWebSocketHandler extends TextWebSocketHandler {

    // deviceId -> open session, so sendCommand() can reach a specific
    // connected robot. In-memory only - fine for a single backend
    // instance; would need a shared store (e.g. Redis pub/sub) behind a
    // load balancer with more than one instance.
    private final Map<String, WebSocketSession> activeSessions =
            new ConcurrentHashMap<>();

    private final RobotService robotService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        String deviceId = deviceIdOf(session);

        activeSessions.put(deviceId, session);
        robotService.updateStatusByDeviceId(deviceId, RobotStatus.IDLE);

        log.info("Robot connected: {}", deviceId);
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {

        String deviceId = deviceIdOf(session);

        RobotStatus status = parseStatusOrNull(message.getPayload());

        if (status == null) {

            log.warn(
                    "Ignoring unrecognized status '{}' from robot {}",
                    message.getPayload(),
                    deviceId
            );
            return;
        }

        robotService.updateStatusByDeviceId(deviceId, status);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus closeStatus
    ) {

        String deviceId = deviceIdOf(session);

        activeSessions.remove(deviceId);
        robotService.updateStatusByDeviceId(deviceId, RobotStatus.OFFLINE);

        log.info(
                "Robot disconnected: {} ({})",
                deviceId,
                closeStatus.getReason()
        );
    }

    // Used by other parts of the backend (e.g. a future admin/AI-driven
    // trigger) to push a command to a specific connected robot.
    // Returns false if that robot isn't currently connected.
    public boolean sendCommand(String deviceId, String command) {

        WebSocketSession session = activeSessions.get(deviceId);

        if (session == null || !session.isOpen()) {
            return false;
        }

        try {
            session.sendMessage(new TextMessage(command));
            return true;
        } catch (IOException e) {
            log.error("Failed to send command to robot {}", deviceId, e);
            return false;
        }
    }

    private RobotStatus parseStatusOrNull(String payload) {

        try {
            return RobotStatus.valueOf(payload.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String deviceIdOf(WebSocketSession session) {
        return (String) session.getAttributes().get("deviceId");
    }
}
