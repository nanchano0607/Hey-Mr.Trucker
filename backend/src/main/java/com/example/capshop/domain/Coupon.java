package com.example.capshop.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;              // 쿠폰명 (예: "신규가입 축하 쿠폰")
    
    @Column(nullable = false)
    private String code;              // 쿠폰 코드 (예: "WELCOME10")
    
    @Enumerated(EnumType.STRING)
    private CouponType type;          // 할인 타입 (PERCENTAGE: 퍼센트, AMOUNT: 고정금액)
    
    private Integer discountValue;    // 할인값 (퍼센트면 10, 고정금액이면 5000)
    
    private Long minOrderAmount;      // 최소 주문금액 조건 (null이면 제한없음)
    private Long maxDiscountAmount;   // 최대 할인금액 (퍼센트 할인시 상한선)
    
    private boolean isActive = true;  // 활성화 여부
    private boolean isReusable = false; // 재사용 가능 여부 (기본: 1회용)
    
    @Column(columnDefinition = "TEXT")
    private String description;       // 쿠폰 설명
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Coupon(String name, String code, CouponType type, Integer discountValue, 
                  Long minOrderAmount, Long maxDiscountAmount,
                  String description) {
        this.name = name;
        this.code = code;
        this.type = type;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // 할인 금액 계산
    public Long calculateDiscount(Long orderAmount) {
        if (minOrderAmount != null && orderAmount < minOrderAmount) {
            throw new IllegalArgumentException("최소 주문금액을 만족하지 않습니다.");
        }
        
        Long discount;
        if (type == CouponType.PERCENTAGE) {
            discount = orderAmount * discountValue / 100;
            // 최대 할인금액 제한
            if (maxDiscountAmount != null && discount > maxDiscountAmount) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = (long) discountValue;
        }
        
        return Math.min(discount, orderAmount); // 주문금액 초과 불가
    }
}