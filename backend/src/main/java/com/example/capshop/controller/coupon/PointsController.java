package com.example.capshop.controller.coupon;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.service.coupon.PointsService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.capshop.domain.user.User;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointsController {
    
    private final PointsService pointsService;
    
    // 적립금 조회
    @GetMapping("/me")
    public ResponseEntity<?> getMyPoints(@AuthenticationPrincipal User user) {
        try {
            Long points = pointsService.getPoints(user.getId());
            return ResponseEntity.ok(Map.of("points", points));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}