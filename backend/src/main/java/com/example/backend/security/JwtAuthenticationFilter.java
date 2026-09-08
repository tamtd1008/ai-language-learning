package com.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    // Process JWT authentication for each HTTP request.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Get the Authorization header from the request.
        String authHeader =
                request.getHeader("Authorization");

        // Continue if no Bearer token is provided.
        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // Extract the JWT token from the header.
        String token =
                authHeader.substring(7);

        // Continue if the JWT token is invalid or expired.
        if (!jwtService.isTokenValid(token)) {

            filterChain.doFilter(request, response);
            return;
        }

        // Extract the username from the JWT token.
        String username =
                jwtService.extractUsername(token);

        // Authenticate the user if not already authenticated.
        if (SecurityContextHolder
                .getContext()
                .getAuthentication() == null) {

            // Load user information from the database.
            UserDetails userDetails =
                    userDetailsService
                            .loadUserByUsername(username);

            // Create an authentication object with the user's authorities.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // Add request details to the authentication object.
            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            // Store the authenticated user in the security context.
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        }

        // Continue processing the request.
        filterChain.doFilter(request, response);
    }
}
