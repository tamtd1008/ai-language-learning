package com.example.backend.ws;

import com.example.backend.entity.Robot;
import com.example.backend.repository.RobotRepository;
import com.example.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RobotHandshakeInterceptorTest {

    private JwtService jwtService;
    private RobotRepository robotRepository;
    private RobotHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        robotRepository = mock(RobotRepository.class);
        interceptor = new RobotHandshakeInterceptor(jwtService, robotRepository);
    }

    @Test
    void rejectsMissingAuthorizationWith401() {
        var response = new MockHttpServletResponse();

        assertFalse(handshake(null, response, new HashMap<>()));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    @Test
    void rejectsInvalidTokenWith401() {
        when(jwtService.isTokenValid("invalid")).thenReturn(false);
        var response = new MockHttpServletResponse();

        assertFalse(handshake("Bearer invalid", response, new HashMap<>()));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    @Test
    void rejectsUnknownDeviceWith404() {
        when(jwtService.isTokenValid("valid")).thenReturn(true);
        when(jwtService.extractUsername("valid")).thenReturn("learner");
        when(robotRepository.findByDeviceId("robot-1"))
                .thenReturn(Optional.empty());
        var response = new MockHttpServletResponse();

        assertFalse(handshake("Bearer valid", response, new HashMap<>()));
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    void allowsOwnerAndStoresDeviceId() {
        when(jwtService.isTokenValid("valid")).thenReturn(true);
        when(jwtService.extractUsername("valid")).thenReturn("learner");
        when(robotRepository.findByDeviceId("robot-1"))
                .thenReturn(Optional.of(Robot.builder().deviceId("robot-1").build()));
        when(robotRepository.existsByDeviceIdAndUserUsername("robot-1", "learner"))
                .thenReturn(true);
        var response = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        assertTrue(handshake("Bearer valid", response, attributes));
        assertEquals("robot-1", attributes.get("deviceId"));
    }

    @Test
    void rejectsAnotherUsersRobotWith403() {
        when(jwtService.isTokenValid("valid")).thenReturn(true);
        when(jwtService.extractUsername("valid")).thenReturn("another-user");
        when(robotRepository.findByDeviceId("robot-1"))
                .thenReturn(Optional.of(Robot.builder().deviceId("robot-1").build()));
        when(robotRepository.existsByDeviceIdAndUserUsername("robot-1", "another-user"))
                .thenReturn(false);
        var response = new MockHttpServletResponse();

        assertFalse(handshake("Bearer valid", response, new HashMap<>()));
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    private boolean handshake(
            String authorization,
            MockHttpServletResponse servletResponse,
            Map<String, Object> attributes
    ) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
                "GET", "/ws/robots/robot-1");
        if (authorization != null) {
            servletRequest.addHeader("Authorization", authorization);
        }

        return interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                new TextWebSocketHandler() {},
                attributes
        );
    }
}
