package com.example.backend.config;

import com.example.backend.ws.RobotHandshakeInterceptor;
import com.example.backend.ws.RobotWebSocketHandler;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// ROBOT-06: registers the realtime channel at /ws/robots/{deviceId}.
// Robots connect here (see RobotHandshakeInterceptor for the auth
// check) after registering via POST /api/robots/register.
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RobotWebSocketHandler robotWebSocketHandler;

    private final RobotHandshakeInterceptor robotHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(robotWebSocketHandler, "/ws/robots/{deviceId}")
                .addInterceptors(robotHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
