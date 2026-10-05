package com.example.backend.repository;

import com.example.backend.entity.Robot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {

    Optional<Robot> findByDeviceId(String deviceId);

    // Used to enforce ownership in one query, same pattern as
    // ConversationSessionRepository.findByIdAndUserUsername.
    Optional<Robot> findByIdAndUserUsername(Long id, String username);

    List<Robot> findByUserUsername(String username);
}
