package com.example.capshop.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewStatistics {
    private double averageRating;      // 평균 별점
    private long totalReviews;         // 전체 리뷰 수
    private long photoReviews;         // 포토 리뷰 수
    private Map<Integer, Long> ratingDistribution; // 별점별 리뷰 수
    
    public ReviewStatistics(double averageRating, long totalReviews, long photoReviews) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.photoReviews = photoReviews;
        this.ratingDistribution = new HashMap<>();
        // 1~5점 초기화
        for (int i = 1; i <= 5; i++) {
            this.ratingDistribution.put(i, 0L);
        }
    }
    
    public void setRatingDistribution(Map<Integer, Long> distribution) {
        this.ratingDistribution = distribution;
    }
}
