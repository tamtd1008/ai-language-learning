package com.example.backend.ws;

import com.example.backend.entity.Robot;
import com.example.backend.repository.RobotRepository;
import com.example.backend.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Gatekeeper for ROBOT-06 (the realtime channel at /ws/robots/{deviceId}):
 * requires the same "Authorization: Bearer <JWT>" header as every REST
 * endpoint, then checks that the JWT's user actually owns this deviceId
 * (registered first via POST /api/robots/register). Parses the JWT
 * itself rather than relying on SecurityContextHolder, since it's not
 * verified in this environment whether Spring Security's normal filter
 * chain populates the context for a raw (non-STOMP) WebSocket handshake
 * the same way it does for ordinary HTTP requests - test this path
 * specifically once deployed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RobotHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    private final RobotRepository robotRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {

        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {

            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String username = jwtService.extractUsername(token);

        String deviceId = extractDeviceId(request.getURI().getPath());

        Robot robot = robotRepository.findByDeviceId(deviceId).orElse(null);

        if (robot == null) {

            log.warn(
                    "WebSocket handshake rejected: device '{}' is not " +
                            "registered (call POST /api/robots/register first)",
                    deviceId
            );
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return false;
        }

        if (!robot.getUser().getUsername().equals(username)) {

            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put("deviceId", deviceId);

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // Nothing to do - status/lastSeenAt are updated in
        // RobotWebSocketHandler.afterConnectionEstablished instead,
        // since only a successful handshake reaches that point.
    }

    private String extractDeviceId(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
