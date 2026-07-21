package com.example.capshop.dto;

import java.time.LocalDateTime;

import com.example.capshop.domain.CouponStatus;
import com.example.capshop.domain.UserCoupon;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserCouponResponse {
    private Long id;
    private CouponResponse coupon;
    private CouponStatus status;
    private LocalDateTime obtainedAt;
    private LocalDateTime usedAt;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String usedOrderId;
    private Long discountAmount;
    private boolean isAvailable;
    
    public UserCouponResponse(UserCoupon userCoupon) {
        this.id = userCoupon.getId();
        this.coupon = new CouponResponse(userCoupon.getCoupon());
        this.status = userCoupon.getStatus();
        this.obtainedAt = userCoupon.getObtainedAt();
        this.usedAt = userCoupon.getUsedAt();
        this.validFrom = userCoupon.getValidFrom();
        this.validUntil = userCoupon.getValidUntil();
        this.usedOrderId = userCoupon.getUsedOrder() != null ? 
                          userCoupon.getUsedOrder().getOrderId() : null;
        this.discountAmount = userCoupon.getDiscountAmount();
        this.isAvailable = userCoupon.isAvailable();
    }
}