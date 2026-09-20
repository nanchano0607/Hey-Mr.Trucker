package com.example.capshop.admin.coupon;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.dto.coupon.UserCouponResponse;
import com.example.capshop.service.coupon.UserCouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/user-coupons")
public class AdminUserCouponController {

    private final UserCouponService userCouponService;

    /** 사용자에게 쿠폰 지급 (쿠폰 ID 기준) */
    @PostMapping("/issue/{couponId}")
    public ResponseEntity<?> issueCouponToUserById(
            @PathVariable("couponId") Long couponId,
            @RequestParam("userId") Long userId) {
        try {
            UserCouponResponse userCoupon = userCouponService.issueCouponToUserById(userId, couponId);
            return ResponseEntity.ok(userCoupon);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
