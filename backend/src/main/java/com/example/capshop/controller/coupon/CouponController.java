package com.example.capshop.controller.coupon;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.dto.coupon.CouponResponse;
import com.example.capshop.service.coupon.CouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {
    
    private final CouponService couponService;

    
    // 쿠폰 단일 조회
    @GetMapping("/{couponId}")
    public ResponseEntity<?> getCoupon(@PathVariable("couponId") Long couponId) {
        try {
            CouponResponse coupon = couponService.getCoupon(couponId);
            return ResponseEntity.ok(coupon);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    

}