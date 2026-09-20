package com.example.backend.service;

import com.example.backend.dto.request.UpdateVocabularyProgressRequest;
import com.example.backend.dto.response.VocabularyProgressResponse;
import com.example.backend.entity.User;
import com.example.backend.entity.Vocabulary;
import com.example.backend.entity.VocabularyProgress;
import com.example.backend.entity.VocabularyProgressStatus;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VocabularyProgressRepository;
import com.example.backend.repository.VocabularyRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VocabularyProgressService {

    private final VocabularyProgressRepository vocabularyProgressRepository;

    private final VocabularyRepository vocabularyRepository;

    private final UserRepository userRepository;

    // PROGRESS-04: mark a word as LEARNING or LEARNED for the current
    // user - upserts, so re-marking the same word just updates status.
    public VocabularyProgressResponse markProgress(
            String username,
            Long vocabularyId,
            UpdateVocabularyProgressRequest request
    ) {

        User user = findUserOrThrow(username);

        Vocabulary vocabulary = vocabularyRepository.findById(vocabularyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Vocabulary not found")
                );

        VocabularyProgressStatus status =
                VocabularyProgressStatus.fromString(request.getStatus());

        VocabularyProgress progress = vocabularyProgressRepository
                .findByUserIdAndVocabularyId(user.getId(), vocabularyId)
                .orElseGet(() ->
                        VocabularyProgress.builder()
                                .user(user)
                                .vocabulary(vocabulary)
                                .build()
                );

        progress.setStatus(status);

        return toResponse(vocabularyProgressRepository.save(progress));
    }

    // statusFilter is optional (null = everything the user is tracking).
    public List<VocabularyProgressResponse> getMyProgress(
            String username,
            String statusFilter
    ) {

        User user = findUserOrThrow(username);

        VocabularyProgressStatus status =
                VocabularyProgressStatus.fromString(statusFilter);

        List<VocabularyProgress> results = status != null
                ? vocabularyProgressRepository
                        .findByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), status)
                : vocabularyProgressRepository
                        .findByUserIdOrderByUpdatedAtDesc(user.getId());

        return results.stream().map(this::toResponse).toList();
    }

    public void removeProgress(String username, Long vocabularyId) {

        User user = findUserOrThrow(username);

        VocabularyProgress progress = vocabularyProgressRepository
                .findByUserIdAndVocabularyId(user.getId(), vocabularyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "This word isn't being tracked yet"
                        )
                );

        vocabularyProgressRepository.delete(progress);
    }

    private User findUserOrThrow(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }

    private VocabularyProgressResponse toResponse(VocabularyProgress progress) {

        return VocabularyProgressResponse.builder()
                .vocabularyId(progress.getVocabulary().getId())
                .word(progress.getVocabulary().getWord())
                .meaning(progress.getVocabulary().getMeaning())
                .status(progress.getStatus().name())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}
