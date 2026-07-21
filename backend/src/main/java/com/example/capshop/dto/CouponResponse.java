package com.example.capshop.dto;

import com.example.capshop.domain.Coupon;
import com.example.capshop.domain.CouponType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CouponResponse {
    private Long id;
    private String name;
    private String code;
    private CouponType type;
    private Integer discountValue;
    private Long minOrderAmount;
    private Long maxDiscountAmount;
    private boolean isActive;
    private boolean isReusable;
    private String description;
    
    public CouponResponse(Coupon coupon) {
        this.id = coupon.getId();
        this.name = coupon.getName();
        this.code = coupon.getCode();
        this.type = coupon.getType();
        this.discountValue = coupon.getDiscountValue();
        this.minOrderAmount = coupon.getMinOrderAmount();
        this.maxDiscountAmount = coupon.getMaxDiscountAmount();
        this.isActive = coupon.isActive();
        this.isReusable = coupon.isReusable();
        this.description = coupon.getDescription();
    }
}