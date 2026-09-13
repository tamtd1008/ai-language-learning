package com.example.backend.service;

import com.example.backend.dto.request.TopicRequest;
import com.example.backend.dto.response.TopicResponse;
import com.example.backend.entity.CefrLevel;
import com.example.backend.entity.Topic;
import com.example.backend.exception.DuplicateResourceException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.TopicRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    // SYSTEM-02: list topics available for learners to pick from.
    public List<TopicResponse> getActiveTopics() {

        return topicRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Admin view - includes inactive topics too.
    public List<TopicResponse> getAllTopics() {

        return topicRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TopicResponse getTopic(Long id) {

        return toResponse(findTopicOrThrow(id));
    }

    public TopicResponse createTopic(TopicRequest request) {

        if (topicRepository.existsByName(request.getName())) {

            throw new DuplicateResourceException(
                    "Topic name already exists"
            );
        }

        Topic topic = Topic.builder()
                .name(request.getName())
                .description(request.getDescription())
                .level(CefrLevel.fromString(request.getLevel()))
                .build();

        return toResponse(topicRepository.save(topic));
    }

    public TopicResponse updateTopic(Long id, TopicRequest request) {

        Topic topic = findTopicOrThrow(id);

        // Only enforce the uniqueness check when the name actually changes.
        if (!topic.getName().equals(request.getName())
                && topicRepository.existsByName(request.getName())) {

            throw new DuplicateResourceException(
                    "Topic name already exists"
            );
        }

        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic.setLevel(CefrLevel.fromString(request.getLevel()));

        return toResponse(topicRepository.save(topic));
    }

    // Soft delete - keeps history (sessions referencing this topic) intact.
    public void deactivateTopic(Long id) {

        Topic topic = findTopicOrThrow(id);
        topic.setActive(false);
        topicRepository.save(topic);
    }

    private Topic findTopicOrThrow(Long id) {

        return topicRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Topic not found"
                        )
                );
    }

    private TopicResponse toResponse(Topic topic) {

        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .level(topic.getLevel() != null ? topic.getLevel().name() : null)
                .active(topic.isActive())
                .createdAt(topic.getCreatedAt())
                .build();
    }
}
