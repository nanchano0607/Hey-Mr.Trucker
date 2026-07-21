package com.example.capshop.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.Story;
import com.example.capshop.service.StoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/story")
public class StoryController {

    private final StoryService storyService;

    @GetMapping
    public Map<String, String> getStory() {
        Story story = storyService.getStory();
        return Map.of(
            "backgroundImage", story.getBackgroundImage() != null ? story.getBackgroundImage() : "",
            "contentImage", story.getContentImage() != null ? story.getContentImage() : ""
        );
    }
}
