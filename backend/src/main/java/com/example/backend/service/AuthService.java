package com.example.backend.service;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.response.AuthResponse;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    // AUTH-01: Register
    public void register(RegisterRequest request) {

        // Check whether the username is already registered.
        if (userRepository.existsByUsername(
                request.getUsername())) {

            throw new RuntimeException(
                    "Username already exists"
            );
        }

        // Check whether the email is already registered.
        if (userRepository.existsByEmail(
                request.getEmail())) {

            throw new RuntimeException(
                    "Email already exists"
            );
        }

        // Create a new user with an encrypted password.
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .enabled(true)
                .build();

        // Save the new user to the database.
        userRepository.save(user);
    }

    // AUTH-02: Login
    public AuthResponse login(LoginRequest request) {

        // Authenticate the user with username and password.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Load the authenticated user from the database.
        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );

        // Generate a JWT token for the authenticated user.
        String token = jwtService.generateToken(
                user.getUsername()
        );

        // Return the JWT token and user information.
        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name()
        );
    }
}
