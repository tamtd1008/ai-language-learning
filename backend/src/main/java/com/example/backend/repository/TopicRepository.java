package com.example.backend.repository;

import com.example.backend.entity.Topic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByActiveTrue();

    Optional<Topic> findByName(String name);

    boolean existsByName(String name);
}
