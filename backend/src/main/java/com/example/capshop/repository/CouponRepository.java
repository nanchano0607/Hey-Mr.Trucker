package com.example.capshop.repository;

// ...existing code...
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
// ...existing code...
import org.springframework.stereotype.Repository;

import com.example.capshop.domain.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    // 쿠폰 코드로 조회
    Optional<Coupon> findByCode(String code);
    
    // 활성화된 쿠폰들 조회 (유효기간 조건 제거)
    List<Coupon> findByIsActiveOrderByCreatedAtDesc(boolean isActive);
    
    // 쿠폰명으로 검색
    List<Coupon> findByNameContainingIgnoreCase(String name);
}