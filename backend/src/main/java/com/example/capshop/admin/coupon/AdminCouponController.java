package com.example.capshop.admin.coupon;

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

import com.example.capshop.dto.coupon.CouponCreateRequest;
import com.example.capshop.dto.coupon.CouponResponse;
import com.example.capshop.service.coupon.CouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllCoupons() {
        try {
            List<CouponResponse> coupons = couponService.getAllCoupons();
            return ResponseEntity.ok(coupons);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createCoupon(@RequestBody CouponCreateRequest request) {
        try {
            CouponResponse coupon = couponService.createCoupon(request);
            return ResponseEntity.ok(coupon);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<?> deleteCoupon(@PathVariable("couponId") Long couponId) {
        try {
            couponService.deleteCoupon(couponId);
            return ResponseEntity.ok(Map.of("message", "쿠폰이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
