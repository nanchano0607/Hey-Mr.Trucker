package com.example.capshop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.dto.ReviewCreateRequest;
import com.example.capshop.dto.ReviewResponse;
import com.example.capshop.dto.ReviewStatistics;
import com.example.capshop.dto.ReviewUpdateRequest;
import com.example.capshop.service.ReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Slf4j
public class ReviewController {
    
    private final ReviewService reviewService;
    
    // 리뷰 작성 가능 여부 확인 - 주문과 상품에 대해 이미 리뷰가 작성되었는지 체크
    @GetMapping("/api/reviews/check")
    public ResponseEntity<?> checkReviewExists(
            @RequestParam(name = "orderId") Long orderId,
            @RequestParam(name = "productId") Long productId) {
        try {
            boolean exists = reviewService.checkReviewExists(orderId, productId);
            return ResponseEntity.ok(Map.of(
                "canWrite", !exists,
                "alreadyReviewed", exists
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 리뷰 작성
    @PostMapping("/api/reviews")
    public ResponseEntity<?> createReview(@RequestBody ReviewCreateRequest request) {
        try {
            ReviewResponse response = reviewService.createReview(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 리뷰 수정
    @PutMapping("/api/reviews/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestParam(name = "userId") Long userId,
            @RequestBody ReviewUpdateRequest request) {
        try {
            ReviewResponse response = reviewService.updateReview(reviewId, userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 리뷰 삭제 (본인)
    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestParam(name = "userId") Long userId) {
        try {
            reviewService.deleteReview(reviewId, userId);
            return ResponseEntity.ok(Map.of("message", "리뷰가 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 리뷰 단일 조회
    @GetMapping("/api/reviews/{reviewId}")
    public ResponseEntity<?> getReview(@PathVariable("reviewId") Long reviewId) {
        try {
            ReviewResponse response = reviewService.getReview(reviewId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 특정 상품의 모든 리뷰 조회
    @GetMapping("/api/reviews/product/{productId}")
    public ResponseEntity<?> getReviewsByProduct(
            @PathVariable("productId") Long productId,
            @RequestParam(name = "sortBy", required = false, defaultValue = "recent") String sortBy) {
        try {
            List<ReviewResponse> reviews = reviewService.getReviewsByProduct(productId, sortBy);
            return ResponseEntity.ok(reviews);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 모든 리뷰 조회 (상품 구분 없이, 비로그인 접근 허용)
    @GetMapping("/api/reviews")
    public ResponseEntity<?> getAllReviews() {
        try {
            List<ReviewResponse> reviews = reviewService.getAllReviews();
            return ResponseEntity.ok(reviews);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // 특정 상품의 포토 리뷰 조회
    @GetMapping("/api/reviews/product/{productId}/photos")
    public ResponseEntity<?> getPhotoReviewsByProduct(@PathVariable("productId") Long productId) {
        try {
            List<ReviewResponse> reviews = reviewService.getPhotoReviewsByProduct(productId);
            return ResponseEntity.ok(reviews);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 특정 상품의 리뷰 통계
    @GetMapping("/api/reviews/product/{productId}/statistics")
    public ResponseEntity<?> getReviewStatistics(@PathVariable("productId") Long productId) {
        log.info("GET /api/reviews/product/{}/statistics called", productId);
        try {
            ReviewStatistics statistics = reviewService.getReviewStatistics(productId);
            log.info("Statistics for productId {}: {}", productId, statistics);
            return ResponseEntity.ok(statistics);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for productId {}: {}", productId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while getting statistics for productId {}", productId, e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }
    
    // 특정 사용자의 모든 리뷰 조회
    @GetMapping("/api/reviews/user/{userId}")
    public ResponseEntity<?> getReviewsByUser(@PathVariable("userId") Long userId) {
        try {
            List<ReviewResponse> reviews = reviewService.getReviewsByUser(userId);
            return ResponseEntity.ok(reviews);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 관리자: 리뷰 삭제 (관리자 권한)
    @DeleteMapping("/api/reviews/admin/{reviewId}")
    public ResponseEntity<?> deleteReviewByAdmin(@PathVariable("reviewId") Long reviewId) {
        try {
            reviewService.deleteReviewByAdmin(reviewId);
            return ResponseEntity.ok(Map.of("message", "리뷰가 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
