package com.example.capshop.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.service.PointsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointsController {
    
    private final PointsService pointsService;
    
    // 적립금 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPoints(@PathVariable("userId") Long userId) {
        try {
            Long points = pointsService.getPoints(userId);
            return ResponseEntity.ok(Map.of("points", points));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}