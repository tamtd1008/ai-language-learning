package com.example.backend.config;

import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default admin account on startup if one doesn't exist yet.
 * Runs every time the app starts - cheap no-op once the account exists,
 * but means you never have to manually INSERT an admin after resetting
 * the database in development.
 *
 * Change app.admin.* (or the ADMIN_* env vars) before deploying anywhere
 * real - the defaults are for local dev only.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }

        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .firstName("Admin")
                .lastName("")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);

        log.info(
                "Seeded default admin account (username='{}'). " +
                        "Change the password after first login.",
                adminUsername
        );
    }
}
