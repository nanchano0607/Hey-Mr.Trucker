package com.example.capshop.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.capshop.domain.Review;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    private Long orderId;
    private String selectedSize;     // 구매한 상품 사이즈
    private int rating;
    private String content;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public ReviewResponse(Review review) {
        this.id = review.getId();
        this.userId = review.getUser().getId();
        this.userName = review.getUser().getName();
        this.productId = review.getProduct().getId();
        this.productName = review.getProduct().getName();
        this.orderId = review.getOrder() != null ? review.getOrder().getId() : null;
        this.rating = review.getRating();
        this.content = review.getContent();
        this.imageUrls = review.getImageUrls();
        this.createdAt = review.getCreatedAt();
        this.updatedAt = review.getUpdatedAt();
        
        // 주문에서 해당 상품의 사이즈 찾기
        if (review.getOrder() != null && review.getOrder().getOrderItems() != null) {
            this.selectedSize = review.getOrder().getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(review.getProduct().getId()))
                .map(item -> item.getSelectedSize())
                .findFirst()
                .orElse("사이즈 정보 없음");
        } else {
            this.selectedSize = "사이즈 정보 없음";
        }
    }
}
