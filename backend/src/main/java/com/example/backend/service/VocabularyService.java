package com.example.backend.service;

import com.example.backend.dto.request.VocabularyRequest;
import com.example.backend.dto.response.VocabularyResponse;
import com.example.backend.entity.CefrLevel;
import com.example.backend.entity.Topic;
import com.example.backend.entity.Vocabulary;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.TopicRepository;
import com.example.backend.repository.VocabularyRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;

    private final TopicRepository topicRepository;

    // LEARN-02: list vocabulary available to learners, optionally
    // narrowed down by topic and/or level.
    public List<VocabularyResponse> getVocabulary(Long topicId, String levelStr) {

        CefrLevel level = CefrLevel.fromString(levelStr);

        List<Vocabulary> results;

        if (topicId != null && level != null) {
            results = vocabularyRepository
                    .findByActiveTrueAndTopicIdAndLevel(topicId, level);
        } else if (topicId != null) {
            results = vocabularyRepository.findByActiveTrueAndTopicId(topicId);
        } else if (level != null) {
            results = vocabularyRepository.findByActiveTrueAndLevel(level);
        } else {
            results = vocabularyRepository.findByActiveTrue();
        }

        return results.stream().map(this::toResponse).toList();
    }

    public List<VocabularyResponse> getAllVocabulary() {

        return vocabularyRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public VocabularyResponse getVocabularyItem(Long id) {

        return toResponse(findVocabularyOrThrow(id));
    }

    public VocabularyResponse createVocabulary(VocabularyRequest request) {

        Vocabulary vocabulary = Vocabulary.builder()
                .word(request.getWord())
                .meaning(request.getMeaning())
                .example(request.getExample())
                .pronunciation(request.getPronunciation())
                .level(CefrLevel.fromString(request.getLevel()))
                .topic(resolveTopic(request.getTopicId()))
                .build();

        return toResponse(vocabularyRepository.save(vocabulary));
    }

    public VocabularyResponse updateVocabulary(Long id, VocabularyRequest request) {

        Vocabulary vocabulary = findVocabularyOrThrow(id);

        vocabulary.setWord(request.getWord());
        vocabulary.setMeaning(request.getMeaning());
        vocabulary.setExample(request.getExample());
        vocabulary.setPronunciation(request.getPronunciation());
        vocabulary.setLevel(CefrLevel.fromString(request.getLevel()));
        vocabulary.setTopic(resolveTopic(request.getTopicId()));

        return toResponse(vocabularyRepository.save(vocabulary));
    }

    // Soft delete - keeps history (e.g. words already shown to a
    // learner) intact even after a word is retired from the active list.
    public void deactivateVocabulary(Long id) {

        Vocabulary vocabulary = findVocabularyOrThrow(id);
        vocabulary.setActive(false);
        vocabularyRepository.save(vocabulary);
    }

    private Topic resolveTopic(Long topicId) {

        if (topicId == null) {
            return null;
        }

        return topicRepository.findById(topicId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Topic not found"
                        )
                );
    }

    private Vocabulary findVocabularyOrThrow(Long id) {

        return vocabularyRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vocabulary not found"
                        )
                );
    }

    private VocabularyResponse toResponse(Vocabulary vocabulary) {

        Topic topic = vocabulary.getTopic();

        return VocabularyResponse.builder()
                .id(vocabulary.getId())
                .word(vocabulary.getWord())
                .meaning(vocabulary.getMeaning())
                .example(vocabulary.getExample())
                .pronunciation(vocabulary.getPronunciation())
                .level(
                        vocabulary.getLevel() != null
                                ? vocabulary.getLevel().name()
                                : null
                )
                .topicId(topic != null ? topic.getId() : null)
                .topicName(topic != null ? topic.getName() : null)
                .active(vocabulary.isActive())
                .createdAt(vocabulary.getCreatedAt())
                .build();
    }
}
