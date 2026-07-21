package com.example.capshop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.dto.CouponCreateRequest;
import com.example.capshop.dto.CouponResponse;
import com.example.capshop.service.CouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {
    
    private final CouponService couponService;
    
    // 모든 쿠폰 조회 (관리자)
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllCoupons() {
        try {
            List<CouponResponse> coupons = couponService.getAllCoupons();
            return ResponseEntity.ok(coupons);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    
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
    

    // 쿠폰 생성 (관리자)
    @PostMapping("/admin")
    public ResponseEntity<?> createCoupon(@RequestBody CouponCreateRequest request) {
        try {
            CouponResponse coupon = couponService.createCoupon(request);
            return ResponseEntity.ok(coupon);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    
    // 쿠폰 삭제 (관리자)
    @DeleteMapping("/admin/{couponId}")
    public ResponseEntity<?> deleteCoupon(@PathVariable("couponId") Long couponId) {
        try {
            couponService.deleteCoupon(couponId);
            return ResponseEntity.ok(Map.of("message", "쿠폰이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    

}