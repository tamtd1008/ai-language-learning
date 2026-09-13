package com.example.backend.repository;

import com.example.backend.entity.AiConfig;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiConfigRepository extends JpaRepository<AiConfig, Long> {

    Optional<AiConfig> findByActiveTrue();

    Optional<AiConfig> findByName(String name);

    boolean existsByName(String name);
}
