package com.example.capshop.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.Review;
import com.example.capshop.domain.User;
import com.example.capshop.domain.order.Order;
import com.example.capshop.dto.ReviewCreateRequest;
import com.example.capshop.dto.ReviewResponse;
import com.example.capshop.dto.ReviewStatistics;
import com.example.capshop.dto.ReviewUpdateRequest;
import com.example.capshop.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final PointsService pointsService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReviewService.class);
    
    // 리뷰 작성 가능 여부 확인 - 특정 주문의 특정 상품에 대해 이미 리뷰가 작성되었는지 체크
    @Transactional(readOnly = true)
    public boolean checkReviewExists(Long orderId, Long productId) {
        Order order = orderService.getOrderDetail(orderId);
        if (order == null) {
            throw new IllegalArgumentException("주문을 찾을 수 없습니다.");
        }
        
        Product product = productService.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다.");
        }
        
        // 특정 주문의 특정 상품에 대한 리뷰 존재 여부 확인
        Optional<Review> existingReview = reviewRepository.findByOrderAndProduct(order, product);
        return existingReview.isPresent();
    }
    
    // 리뷰 작성
    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request) {
        // 사용자, 상품, 주문 조회
        User user = userService.findById(request.getUserId());
        Product product = productService.findById(request.getProductId());
        Order order = orderService.getOrderDetail(request.getOrderId());
        
        // 유효성 검사
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        if (product == null) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다.");
        }
        if (order == null) {
            throw new IllegalArgumentException("주문을 찾을 수 없습니다.");
        }
        
        // 주문한 사용자인지 확인
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인의 주문에만 리뷰를 작성할 수 있습니다.");
        }
        
        // 이미 이 상품에 대한 리뷰를 작성했는지 확인
        Optional<Review> existingReview = reviewRepository.findByOrderAndProduct(order, product);
        if (existingReview.isPresent()) {
            throw new IllegalArgumentException("이미 이 상품에 대한 리뷰를 작성했습니다.");
        }
        
        // 별점 유효성 검사
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이여야 합니다.");
        }
        
        // 리뷰 생성
        Review review = new Review(user, product, order, request.getRating(), request.getContent(), request.getImageUrls());
        
        Review savedReview = reviewRepository.save(review);
        
        // 리뷰 작성 시 자동으로 구매확정 처리
        try {
            if (!order.isConfirmed()) {
                orderService.confirmPurchase(order.getId(), user.getId());
            }
        } catch (Exception e) {
            // 구매확정이 실패해도 리뷰 작성은 성공으로 처리
        }
        
        // 리뷰 작성 시 보너스 적립금 지급
        try {
            pointsService.addReviewPoints(user.getId());
        } catch (Exception e) {
            // 적립금 지급 실패해도 리뷰 작성은 성공으로 처리
        }
        
        return new ReviewResponse(savedReview);
    }
    
    // 리뷰 수정
    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long userId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        
        // 작성자 본인인지 확인
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 리뷰만 수정할 수 있습니다.");
        }
        
        // 별점 유효성 검사
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이여야 합니다.");
        }
        
        review.updateReview(request.getRating(), request.getContent(), request.getImageUrls());
        Review updatedReview = reviewRepository.save(review);
        
        return new ReviewResponse(updatedReview);
    }
    
    // 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        
        // 작성자 본인인지 확인
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 리뷰만 삭제할 수 있습니다.");
        }
        
        reviewRepository.delete(review);
    }
    
    // 관리자: 리뷰 삭제
    @Transactional
    public void deleteReviewByAdmin(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        
        reviewRepository.delete(review);
    }
    
    // 특정 상품의 모든 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(Long productId, String sortBy) {
        Product product = productService.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다.");
        }
        
        List<Review> reviews;
        if ("rating".equals(sortBy)) {
            reviews = reviewRepository.findByProductOrderByRatingDescCreatedAtDesc(product);
        } else {
            reviews = reviewRepository.findByProductOrderByCreatedAtDesc(product);
        }
        
        return reviews.stream()
                .map(ReviewResponse::new)
                .collect(Collectors.toList());
    }
    
    // 포토 리뷰만 조회
    @Transactional(readOnly = true)
    public List<ReviewResponse> getPhotoReviewsByProduct(Long productId) {
        Product product = productService.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다.");
        }
        
        List<Review> reviews = reviewRepository.findPhotoReviewsByProduct(product);
        return reviews.stream()
                .map(ReviewResponse::new)
                .collect(Collectors.toList());
    }
    
    // 특정 사용자의 모든 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByUser(Long userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        
        List<Review> reviews = reviewRepository.findByUserOrderByCreatedAtDesc(user);
        return reviews.stream()
                .map(ReviewResponse::new)
                .collect(Collectors.toList());
    }
    
    // 특정 상품의 리뷰 통계 조회
    @Transactional(readOnly = true)
    public ReviewStatistics getReviewStatistics(Long productId) {
        log.info("getReviewStatistics called with productId={}", productId);
        Product product = productService.findById(productId);
        if (product == null) {
            log.warn("Product not found for id={}", productId);
            throw new IllegalArgumentException("상품을 찾을 수 없습니다.");
        }
        
        // 평균 별점
        Double avgRating = reviewRepository.getAverageRatingByProduct(product);
        double averageRating = avgRating != null ? avgRating : 0.0;
        
        // 전체 리뷰 수
        long totalReviews = reviewRepository.countByProduct(product);
        
        // 포토 리뷰 수
        long photoReviews = reviewRepository.findPhotoReviewsByProduct(product).size();
        
        ReviewStatistics statistics = new ReviewStatistics(averageRating, totalReviews, photoReviews);
        
        // 별점별 리뷰 수
        List<Object[]> ratingCounts = reviewRepository.countByRatingGroupByProduct(product);
        Map<Integer, Long> distribution = ratingCounts.stream()
                .collect(Collectors.toMap(
                    arr -> (Integer) arr[0],
                    arr -> (Long) arr[1]
                ));
        statistics.setRatingDistribution(distribution);
        log.info("Review statistics computed for productId={}: {}", productId, statistics);
        
        return statistics;
    }
    
    // 리뷰 단일 조회
    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        
        return new ReviewResponse(review);
    }

    // 모든 리뷰 조회 (상품 구분 없이, 최신순)
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        List<Review> reviews = reviewRepository.findAll();
        return reviews.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(ReviewResponse::new)
                .collect(Collectors.toList());
    }
}
