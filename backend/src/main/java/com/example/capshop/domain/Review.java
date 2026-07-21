package com.example.capshop.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.example.capshop.domain.order.Order;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;              // 리뷰 작성자
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;          // 리뷰 대상 상품
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;            // 구매 확인용 (구매한 사람만 리뷰 작성 가능)
    
    @Column(nullable = false)
    private int rating;             // 별점 (1~5)
    
    @Column(columnDefinition = "TEXT")
    private String content;         // 리뷰 내용
    
    @ElementCollection
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url")
    private List<String> imageUrls; // 리뷰 이미지들
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 생성자
    public Review(User user, Product product, Order order, int rating, String content, List<String> imageUrls) {
        this.user = user;
        this.product = product;
        this.order = order;
        this.rating = rating;
        this.content = content;
        this.imageUrls = imageUrls;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // 리뷰 수정
    public void updateReview(int rating, String content, List<String> imageUrls) {
        this.rating = rating;
        this.content = content;
        this.imageUrls = imageUrls;
        this.updatedAt = LocalDateTime.now();
    }
}
