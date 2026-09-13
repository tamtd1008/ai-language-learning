package com.example.backend.service;

import com.example.backend.dto.request.UpdateLearnerLevelRequest;
import com.example.backend.dto.response.LearnerLevelResponse;
import com.example.backend.entity.CefrLevel;
import com.example.backend.entity.LearnerLevel;
import com.example.backend.entity.LevelSource;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.LearnerLevelRepository;
import com.example.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LearnerLevelService {

    private final LearnerLevelRepository learnerLevelRepository;

    private final UserRepository userRepository;

    // LEARN-01: returns the learner's current level, or an empty
    // response (level = null) if it hasn't been set yet.
    public LearnerLevelResponse getMyLevel(String username) {

        User user = findUserOrThrow(username);

        return learnerLevelRepository.findByUserId(user.getId())
                .map(this::toResponse)
                .orElseGet(() ->
                        LearnerLevelResponse.builder().build()
                );
    }

    // LEARN-01: self-assessment. A real placement test / AI-driven
    // assessment can overwrite this later (LevelSource distinguishes
    // how the value was set) without changing this API's shape.
    public LearnerLevelResponse setMyLevel(
            String username,
            UpdateLearnerLevelRequest request
    ) {

        User user = findUserOrThrow(username);

        CefrLevel level = CefrLevel.fromString(request.getLevel());

        LearnerLevel learnerLevel = learnerLevelRepository
                .findByUserId(user.getId())
                .orElseGet(() ->
                        LearnerLevel.builder()
                                .user(user)
                                .build()
                );

        learnerLevel.setLevel(level);
        learnerLevel.setSource(LevelSource.SELF_ASSESSED);

        return toResponse(learnerLevelRepository.save(learnerLevel));
    }

    private User findUserOrThrow(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private LearnerLevelResponse toResponse(LearnerLevel learnerLevel) {

        return LearnerLevelResponse.builder()
                .level(learnerLevel.getLevel().name())
                .source(learnerLevel.getSource().name())
                .updatedAt(learnerLevel.getUpdatedAt())
                .build();
    }
}
