package com.example.capshop.domain;

import java.time.LocalDateTime;

import com.example.capshop.domain.order.Order;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class UserCoupon {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;
    
    @Enumerated(EnumType.STRING)
    private CouponStatus status;      // 쿠폰 상태 (AVAILABLE, USED, EXPIRED)
    
    private LocalDateTime obtainedAt;  // 쿠폰 획득일
    private LocalDateTime usedAt;      // 쿠폰 사용일
    private LocalDateTime validFrom;   // 유효 시작일
    private LocalDateTime validUntil;  // 유효 만료일
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order usedOrder;          // 사용된 주문 (사용시에만)
    
    private Long discountAmount;      // 실제 할인받은 금액 (사용시에만)
    
    public UserCoupon(User user, Coupon coupon) {
        this.user = user;
        this.coupon = coupon;
        this.status = CouponStatus.AVAILABLE;
        this.obtainedAt = LocalDateTime.now();
        // 기본 유효기간: 지급 시점부터 30일 (예시)
        this.validFrom = LocalDateTime.now();
        this.validUntil = LocalDateTime.now().plusDays(30);
    }
    
    // 쿠폰 사용
    public void useCoupon(Order order, Long discountAmount) {
        if (status != CouponStatus.AVAILABLE) {
            throw new IllegalStateException("사용할 수 없는 쿠폰입니다.");
        }
        if (!isValid()) {
            this.status = CouponStatus.EXPIRED;
            throw new IllegalStateException("만료된 쿠폰입니다.");
        }
        
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.usedOrder = order;
        this.discountAmount = discountAmount;
    }
    
    // 쿠폰 만료 처리
    public void expireCoupon() {
        if (status == CouponStatus.AVAILABLE) {
            this.status = CouponStatus.EXPIRED;
        }
    }

    // 주문 취소/환불로 쿠폰 사용을 되돌림
    public void restoreAfterOrderCancel() {
        // 이미 EXPIRED면 그대로 유지
        if (this.status == CouponStatus.EXPIRED) {
            this.usedAt = null;
            this.usedOrder = null;
            this.discountAmount = null;
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (this.validUntil != null && !this.validUntil.isAfter(now)) {
            this.status = CouponStatus.EXPIRED;
        } else {
            this.status = CouponStatus.AVAILABLE;
        }
        this.usedAt = null;
        this.usedOrder = null;
        this.discountAmount = null;
    }
    
    // 사용 가능한지 확인
    public boolean isAvailable() {
    return isValid();
    }
    
    // 쿠폰 유효성 검사
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return status == CouponStatus.AVAILABLE &&
               validFrom.isBefore(now) &&
               validUntil.isAfter(now);
    }
}