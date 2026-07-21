package com.example.capshop.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.capshop.domain.Story;
import com.example.capshop.repository.StoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoryService {
    private final StoryRepository storyRepository;
    
    @Value("${app.upload-dir}")
    private String uploadBaseDir;
    
    private static final Long STORY_ID = 1L;
    private static final String CAP_DIR = "cap";

    /**
     * 스토리 조회
     * @author 김찬호
     * @param none
     * @return 스토리 객체 얻기
     */
    @Transactional
    public Story getStory() {
        return storyRepository.findById(STORY_ID)
                .orElseGet(this::createDefaultStory);
    }

    private Story createDefaultStory() {
        Story story = new Story("", "");
        return storyRepository.save(story);
    }

    @Transactional
    public Story createOrUpdateStory(String backgroundImage, String contentImage) {
        Story story = storyRepository.findById(STORY_ID).orElse(null);
        
        // Story가 없으면 새로 생성
        if (story == null) {
            story = new Story(backgroundImage, contentImage);
            story = storyRepository.save(story);
            return story;
        }
        
        if (backgroundImage != null) {
            // 기존 배경이미지 삭제
            if (story.getBackgroundImage() != null && !story.getBackgroundImage().isBlank()) {
                deleteImageFile(story.getBackgroundImage());
            }
            story.setBackgroundImage(backgroundImage);
        }
        if (contentImage != null) {
            // 기존 콘텐츠이미지 삭제
            if (story.getContentImage() != null && !story.getContentImage().isBlank()) {
                deleteImageFile(story.getContentImage());
            }
            story.setContentImage(contentImage);
        }
        story.setUpdatedAt(LocalDateTime.now());
        
        return storyRepository.save(story);
    }

    private void deleteImageFile(String filename) {
        try {
            Path filePath = Paths.get(uploadBaseDir, CAP_DIR, filename).normalize();
            Path base = Paths.get(uploadBaseDir).normalize();
            
            // 경로 검증
            if (!filePath.startsWith(base)) {
                return;
            }
            
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 파일 삭제 실패해도 계속 진행 (로그만 남김)
            System.err.println("Failed to delete image file: " + filename + ", " + e.getMessage());
        }
    }
}

