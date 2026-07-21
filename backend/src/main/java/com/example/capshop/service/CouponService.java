package com.example.capshop.service;

// ...existing code...
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.capshop.domain.Coupon;
import com.example.capshop.dto.CouponCreateRequest;
import com.example.capshop.dto.CouponResponse;
import com.example.capshop.repository.CouponRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {
    
    private final CouponRepository couponRepository;
    
    // 쿠폰 생성 (관리자)
    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        // 쿠폰 코드 중복 확인
        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 쿠폰 코드입니다.");
        }
        
        Coupon coupon = new Coupon(
            request.getName(),
            request.getCode(),
            request.getType(),
            request.getDiscountValue(),
            request.getMinOrderAmount(),
            request.getMaxDiscountAmount(),
            request.getDescription()
        );
        
        coupon.setReusable(request.isReusable());
        
        Coupon savedCoupon = couponRepository.save(coupon);
        return new CouponResponse(savedCoupon);
    }
    
    // 모든 쿠폰 조회 (관리자)
    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(CouponResponse::new)
                .collect(Collectors.toList());
    }
    
    // 활성화된 쿠폰들만 조회
    @Transactional(readOnly = true)
    public List<CouponResponse> getActiveCoupons() {
        return couponRepository.findByIsActiveOrderByCreatedAtDesc(true).stream()
                .map(CouponResponse::new)
                .collect(Collectors.toList());
    }
    
    // 현재 유효한 쿠폰들만 조회
    @Transactional(readOnly = true)
    public List<CouponResponse> getValidCoupons() {
        return couponRepository.findByIsActiveOrderByCreatedAtDesc(true).stream()
                .map(CouponResponse::new)
                .collect(Collectors.toList());
    }
    
    // 쿠폰 코드로 조회
    @Transactional(readOnly = true)
    public CouponResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 코드입니다."));
        return new CouponResponse(coupon);
    }
    
    // 쿠폰 단일 조회
    @Transactional(readOnly = true)
    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
        return new CouponResponse(coupon);
    }
    
    // 쿠폰 활성화/비활성화 (관리자)
    @Transactional
    public void toggleCouponStatus(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
        
        coupon.setActive(!coupon.isActive());
        couponRepository.save(coupon);
    }
    
    // 쿠폰 삭제 (관리자)
    @Transactional
    public void deleteCoupon(Long couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new IllegalArgumentException("존재하지 않는 쿠폰입니다.");
        }
        couponRepository.deleteById(couponId);
    }
    
    // 내부 사용용 - Entity 반환
    @Transactional(readOnly = true)
    public Coupon findById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    }
    
    // 내부 사용용 - 코드로 Entity 반환
    @Transactional(readOnly = true)
    public Coupon findByCode(String code) {
        return couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 코드입니다."));
    }
}