package com.example.capshop.controller.coupon;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.example.capshop.dto.coupon.UserCouponResponse;
import com.example.capshop.service.coupon.UserCouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-coupons")
public class UserCouponController {
    
    private final UserCouponService userCouponService;
    
    // 사용자의 모든 쿠폰 조회
    @GetMapping("/me")
    public ResponseEntity<?> getMyCoupons(@AuthenticationPrincipal com.example.capshop.domain.user.User user) {
        try {
            List<UserCouponResponse> coupons = userCouponService.getUserCoupons(user.getId());
            return ResponseEntity.ok(coupons);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 사용자의 사용 가능한 쿠폰들만 조회
    @GetMapping("/me/available")
    public ResponseEntity<?> getMyAvailableCoupons(@AuthenticationPrincipal com.example.capshop.domain.user.User user) {
        try {
            List<UserCouponResponse> coupons = userCouponService.getAvailableUserCoupons(user.getId());
            return ResponseEntity.ok(coupons);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 특정 주문 금액에 적용 가능한 쿠폰들 조회
    @GetMapping("/me/applicable")
    public ResponseEntity<?> getMyApplicableCoupons(
            @AuthenticationPrincipal com.example.capshop.domain.user.User user,
            @RequestParam("orderAmount") Long orderAmount) {
        try {
            List<UserCouponResponse> coupons = userCouponService.getAvailableCouponsForOrder(user.getId(), orderAmount);
            return ResponseEntity.ok(coupons);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 인증된 사용자가 쿠폰 코드로 본인에게 쿠폰을 발급받음
    @PostMapping("/claim")
    public ResponseEntity<?> claimCoupon(
            @AuthenticationPrincipal com.example.capshop.domain.user.User user,
            @RequestBody Map<String, String> req) {
        try {
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
            }
            String couponCode = req.get("couponCode");
            if (couponCode == null || couponCode.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "쿠폰 코드를 입력해주세요."));
            }
            UserCouponResponse userCoupon = userCouponService.issueCouponToUser(user.getId(), couponCode.trim());
            return ResponseEntity.ok(userCoupon);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    
}
    
