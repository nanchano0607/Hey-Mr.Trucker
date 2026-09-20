package com.example.capshop.repository.content;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.content.Story;

public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findAllByOrderByCreatedAtDesc();

    Optional<Story> findTopByOrderByIdAsc();
}
