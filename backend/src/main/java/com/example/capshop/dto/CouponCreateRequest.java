package com.example.capshop.dto;

import com.example.capshop.domain.CouponType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CouponCreateRequest {
    private String name;
    private String code;
    private CouponType type;
    private Integer discountValue;
    private Long minOrderAmount;
    private Long maxDiscountAmount;
    private boolean isReusable;
    private String description;
}