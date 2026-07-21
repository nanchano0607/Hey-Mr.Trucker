package com.example.capshop.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.capshop.domain.User;
import com.example.capshop.dto.PointsRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointsService {
    
    private final UserService userService;

    @Value("${app.points.order-rate:0.01}")
    private BigDecimal orderRate;
    
    // 적립금 적립
    @Transactional
    public void addPoints(PointsRequest request) {
        User user = userService.findById(request.getUserId());
        
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("적립금은 0보다 커야 합니다.");
        }
        
        user.addPoints(request.getAmount());
        // UserService가 이미 save를 처리한다면 별도 저장 불필요
        // 그렇지 않다면 userRepository.save(user) 필요
    }
    
    // 적립금 사용
    @Transactional
    public void usePoints(PointsRequest request) {
        User user = userService.findById(request.getUserId());
        
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("사용할 적립금은 0보다 커야 합니다.");
        }
        
        user.usePoints(request.getAmount());
        // UserService가 이미 save를 처리한다면 별도 저장 불필요
    }
    
    // 적립금 조회
    @Transactional(readOnly = true)
    public Long getPoints(Long userId) {
        User user = userService.findById(userId);
        return user.getAvailablePoints();
    }
    
    // 주문 완료 시 자동 적립 (주문 금액의 1%)
    @Transactional
    public void addOrderPoints(Long userId, Long orderAmount) {
        Long pointsToAdd = calculateOrderPoints(orderAmount);
        
        if (pointsToAdd > 0) {
            PointsRequest request = new PointsRequest();
            request.setUserId(userId);
            request.setAmount(pointsToAdd);
            request.setReason("주문 완료 적립");
            
            addPoints(request);
        }
    }

    public Long calculateOrderPoints(Long orderAmount) {
        if (orderAmount == null || orderAmount <= 0) {
            return 0L;
        }

        BigDecimal rate = (orderRate == null) ? new BigDecimal("0.01") : orderRate;
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        // 원 단위 정수 적립: (금액 * 비율) 내림
        return BigDecimal.valueOf(orderAmount)
                .multiply(rate)
                .setScale(0, RoundingMode.DOWN)
                .longValue();
    }
    
    // 리뷰 작성 시 보너스 적립금 지급
    @Transactional
    public void addReviewPoints(Long userId) {
        PointsRequest request = new PointsRequest();
        request.setUserId(userId);
        request.setAmount(500L); // 리뷰 작성 시 500원 적립
        request.setReason("리뷰 작성 보너스");
        
        addPoints(request);
    }
}