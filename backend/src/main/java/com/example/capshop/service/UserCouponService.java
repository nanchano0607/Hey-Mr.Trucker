package com.example.capshop.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.capshop.domain.Coupon;
import com.example.capshop.domain.CouponStatus;
import com.example.capshop.domain.User;
import com.example.capshop.domain.UserCoupon;
import com.example.capshop.domain.order.Order;
import com.example.capshop.dto.UserCouponResponse;
import com.example.capshop.repository.UserCouponRepository;
import com.example.capshop.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserCouponService {
    
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;

    @Value("${app.welcome-coupon-code:}")
    private String welcomeCouponCode;

    @Value("${app.welcome-coupon-id:1}")
    private Long welcomeCouponId;
    
    // 사용자에게 쿠폰 지급
    @Transactional
    public UserCouponResponse issueCouponToUser(Long userId, String couponCode) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Coupon coupon = couponService.findByCode(couponCode);
        
        // 재사용 불가 쿠폰인 경우 이미 보유하고 있는지 확인
        if (!coupon.isReusable() && userCouponRepository.existsByUserAndCoupon(user, coupon)) {
            throw new IllegalArgumentException("이미 보유하고 있는 쿠폰입니다.");
        }
        
        // 유효기간 커스터마이징: 지급일로부터 30일
        UserCoupon userCoupon = new UserCoupon(user, coupon);
        userCoupon.setValidFrom(userCoupon.getObtainedAt());
        userCoupon.setValidUntil(userCoupon.getObtainedAt().plusDays(30));
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);
        
        return new UserCouponResponse(savedUserCoupon);
    }
    
    // 사용자에게 쿠폰 지급 (쿠폰 ID로)
    @Transactional
    public UserCouponResponse issueCouponToUserById(Long userId, Long couponId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Coupon coupon = couponService.findById(couponId);
        // issuing coupon to user: user and coupon loaded
        // 재사용 불가 쿠폰인 경우 이미 보유하고 있는지 확인
        if (!coupon.isReusable() && userCouponRepository.existsByUserAndCoupon(user, coupon)) {
            // user already has non-reusable coupon
            throw new IllegalArgumentException("이미 보유하고 있는 쿠폰입니다.");
        }
        // 유효기간 커스터마이징: 지급일로부터 30일
        UserCoupon userCoupon = new UserCoupon(user, coupon);
        userCoupon.setValidFrom(userCoupon.getObtainedAt());
        userCoupon.setValidUntil(userCoupon.getObtainedAt().plusDays(30));
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);
        // coupon issued successfully
        return new UserCouponResponse(savedUserCoupon);
    }
    
    // 사용자의 모든 쿠폰 조회
    @Transactional(readOnly = true)
    public List<UserCouponResponse> getUserCoupons(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        return userCouponRepository.findByUserOrderByObtainedAtDesc(user).stream()
                .map(UserCouponResponse::new)
                .collect(Collectors.toList());
    }
    
    // 사용자의 사용 가능한 쿠폰들 조회
    @Transactional(readOnly = true)
    public List<UserCouponResponse> getAvailableUserCoupons(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        return userCouponRepository.findByUserAndStatus(user, CouponStatus.AVAILABLE).stream()
                .filter(uc -> uc.isAvailable()) // 유효기간도 체크
                .map(UserCouponResponse::new)
                .collect(Collectors.toList());
    }
    
    // 특정 주문 금액에 적용 가능한 쿠폰들 조회
    @Transactional(readOnly = true)
    public List<UserCouponResponse> getAvailableCouponsForOrder(Long userId, Long orderAmount) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        return userCouponRepository.findAvailableCouponsForOrder(user, orderAmount).stream()
                .map(UserCouponResponse::new)
                .collect(Collectors.toList());
    }
    
    // 쿠폰 사용
    @Transactional
    public Long useCoupon(Long userCouponId, Order order) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 쿠폰입니다."));
        
        // 쿠폰 할인 금액 계산
        Long discountAmount = userCoupon.getCoupon().calculateDiscount(resolveCouponOrderAmount(order));
        
        // 쿠폰 사용 처리
        userCoupon.useCoupon(order, discountAmount);
        userCouponRepository.save(userCoupon);
        
        return discountAmount;
    }
    
    // 쿠폰 코드로 직접 사용 (주문 시)
    @Transactional
    public Long useCouponByCode(Long userId, String couponCode, Order order) {
    User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        // 사용자가 보유한 해당 쿠폰 중 사용 가능한 것 찾기
        List<UserCoupon> availableCoupons = userCouponRepository.findByUserAndStatus(user, CouponStatus.AVAILABLE)
                .stream()
                .filter(uc -> uc.getCoupon().getCode().equals(couponCode) && uc.isAvailable())
                .collect(Collectors.toList());
        
        if (availableCoupons.isEmpty()) {
            throw new IllegalArgumentException("사용할 수 있는 쿠폰이 없습니다.");
        }
        
        // 첫 번째 사용 가능한 쿠폰 사용
        UserCoupon userCoupon = availableCoupons.get(0);
        
        // 쿠폰 할인 금액 계산
        Long discountAmount = userCoupon.getCoupon().calculateDiscount(resolveCouponOrderAmount(order));
        
        // 쿠폰 사용 처리
        userCoupon.useCoupon(order, discountAmount);
        userCouponRepository.save(userCoupon);
        
        return discountAmount;
    }
    
    // 만료된 쿠폰들 정리 (스케줄러용)
    @Transactional
    public int expireOldCoupons() {
    List<UserCoupon> availableCoupons = userCouponRepository.findByStatus(CouponStatus.AVAILABLE);

    int expiredCount = 0;
    for (UserCoupon userCoupon : availableCoupons) {

        // isValid() = 유효기간 체크
        if (!userCoupon.isValid()) {
            userCoupon.expireCoupon();  // 상태를 EXPIRED 로 바꾸는 메서드라고 가정
            expiredCount++;
        }
    }
    return expiredCount;
    }
    
    @Transactional
    public Long markCouponUsedOnSuccess(Long userId, Long userCouponId, Order order) {
        UserCoupon uc = userCouponRepository.findById(userCouponId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 쿠폰입니다."));

        if (!uc.getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인 소유의 쿠폰이 아닙니다.");
        }

        // 로그 추가: 쿠폰 상태 확인
        System.out.println("[LOG] 쿠폰 상태: " + uc.getStatus());
        System.out.println("[LOG] 쿠폰 ID: " + userCouponId);
        System.out.println("[LOG] 사용자 ID: " + userId);

        // 이미 사용됨 → 멱등 처리
        if (uc.getStatus() == CouponStatus.USED) {
            Long already = uc.getDiscountAmount() == null ? 0L : uc.getDiscountAmount();
            if (order.getUsedUserCoupon() == null) order.applyCouponDiscount(already, uc);
            System.out.println("[LOG] 이미 사용된 쿠폰: 할인액=" + already);
            return already;
        }
        if (uc.getStatus() != CouponStatus.AVAILABLE) {
            throw new IllegalStateException("쿠폰을 사용할 수 없는 상태입니다.");
        }

        Long discount = uc.getCoupon().calculateDiscount(resolveCouponOrderAmount(order));

        // 로그 추가: 할인 금액 계산
        System.out.println("[LOG] 계산된 할인 금액: " + discount);

        // 1) 쿠폰 USED 전환(사용 주문/금액 기록)
        uc.useCoupon(order, discount);
        userCouponRepository.save(uc);

        // 2) 주문에도 연결 + 합계 반영 (스냅샷 없음)
        order.applyCouponDiscount(discount, uc);

        // 로그 추가: 최종 처리 완료
        System.out.println("[LOG] 쿠폰 사용 완료: 할인액=" + discount);

        return discount;
    }

    private Long resolveCouponOrderAmount(Order order) {
        Long originalAmount = order.getOriginal_price();
        if (originalAmount != null && originalAmount > 0) {
            return originalAmount;
        }

        Long totalAmount = order.getTotal_price();
        return totalAmount == null ? 0L : totalAmount;
    }
    
    // 신규 유저에게 웰컴 쿠폰 자동 지급
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueWelcomeCouponToNewUser(Long userId) {
        // 운영/로컬에서 couponId가 달라질 수 있으므로 code 우선 사용
        if (welcomeCouponCode != null && !welcomeCouponCode.isBlank()) {
            issueCouponToUser(userId, welcomeCouponCode.trim());
            return;
        }
        // code 미설정이면 id로 fallback
        issueCouponToUserById(userId, welcomeCouponId != null ? welcomeCouponId : 1L);
    }
}
