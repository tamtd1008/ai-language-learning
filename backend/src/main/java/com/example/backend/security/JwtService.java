package com.example.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        // Create a secret key for signing and verifying JWTs.
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        // Store the JWT expiration time.
        this.expiration = expiration;
    }

    // Generate a JWT token for the specified username.
    public String generateToken(String username) {

        Date now = new Date();

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey)
                .compact();
    }

    // Extract the username from the JWT token.
    public String extractUsername(String token) {

        return getClaims(token)
                .getSubject();
    }

    // Check whether the JWT token is valid and not expired.
    public boolean isTokenValid(String token) {

        try {

            Claims claims = getClaims(token);

            return !claims.getExpiration()
                    .before(new Date());

        } catch (Exception e) {

            return false;
        }
    }

    // Parse and verify the JWT token.
    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
