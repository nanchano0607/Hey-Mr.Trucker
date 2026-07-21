package com.example.capshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.Review;
import com.example.capshop.domain.User;
import com.example.capshop.domain.order.Order;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // 특정 상품의 모든 리뷰 조회 (최신순)
    List<Review> findByProductOrderByCreatedAtDesc(Product product);
    
    // 특정 상품의 리뷰 조회 (별점 높은 순)
    List<Review> findByProductOrderByRatingDescCreatedAtDesc(Product product);
    
    // 특정 사용자의 모든 리뷰 조회
    List<Review> findByUserOrderByCreatedAtDesc(User user);
    
    // 특정 주문에 대한 리뷰가 이미 있는지 확인
    Optional<Review> findByOrder(Order order);
    
    // 특정 주문의 특정 상품에 대한 리뷰가 이미 있는지 확인
    Optional<Review> findByOrderAndProduct(Order order, Product product);
    
    // 특정 사용자가 특정 상품에 대해 작성한 리뷰가 있는지 확인
    Optional<Review> findByUserAndProduct(User user, Product product);
    
    // 특정 상품의 평균 별점 계산
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product")
    Double getAverageRatingByProduct(@Param("product") Product product);
    
    // 특정 상품의 리뷰 개수
    long countByProduct(Product product);
    
    // 별점별 리뷰 개수
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product = :product GROUP BY r.rating")
    List<Object[]> countByRatingGroupByProduct(@Param("product") Product product);
    
    // 포토 리뷰만 조회 (이미지가 있는 리뷰)
    @Query("SELECT r FROM Review r WHERE r.product = :product AND SIZE(r.imageUrls) > 0 ORDER BY r.createdAt DESC")
    List<Review> findPhotoReviewsByProduct(@Param("product") Product product);
}
