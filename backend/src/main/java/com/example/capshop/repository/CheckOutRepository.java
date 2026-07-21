package com.example.capshop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.capshop.domain.order.CheckOut;

@Repository
public interface CheckOutRepository extends JpaRepository<CheckOut, Long> {
    Optional<CheckOut> findByOrderId(String orderId);
    
    // 오래된 CheckOut 데이터 정리를 위한 메서드들
    List<CheckOut> findByCreatedAtBefore(LocalDateTime createdAt);
    long countByCreatedAtBefore(LocalDateTime createdAt);
}
