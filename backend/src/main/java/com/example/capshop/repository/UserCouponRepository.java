package com.example.capshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.capshop.domain.Coupon;
import com.example.capshop.domain.CouponStatus;
import com.example.capshop.domain.User;
import com.example.capshop.domain.UserCoupon;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    
    // 특정 사용자의 모든 쿠폰 조회
    List<UserCoupon> findByUserOrderByObtainedAtDesc(User user);
    
    // 특정 사용자의 사용 가능한 쿠폰들 조회
    List<UserCoupon> findByUserAndStatus(User user, CouponStatus status);
    
    // 특정 사용자가 특정 쿠폰을 이미 보유하고 있는지 확인
    boolean existsByUserAndCoupon(User user, Coupon coupon);
    
    // 사용 가능한 쿠폰 중에서 특정 주문 금액에 적용 가능한 쿠폰들 조회
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user = :user AND uc.status = 'AVAILABLE' " +
           "AND uc.coupon.isActive = true " +
           "AND (uc.validFrom <= CURRENT_TIMESTAMP AND uc.validUntil > CURRENT_TIMESTAMP) " +
           "AND (uc.coupon.minOrderAmount IS NULL OR uc.coupon.minOrderAmount <= :orderAmount)")
    List<UserCoupon> findAvailableCouponsForOrder(@Param("user") User user, @Param("orderAmount") Long orderAmount);
    
    // 스케줄러용 status available -> expired용
    List<UserCoupon> findByStatus(CouponStatus status);
}