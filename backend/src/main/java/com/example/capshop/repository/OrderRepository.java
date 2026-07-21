package com.example.capshop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.Status;
import com.example.capshop.domain.User;
import com.example.capshop.domain.order.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByStatus(Status status);
    Optional<Order> findByOrderId(String orderId); // 주문번호로 조회
    List<Order> findByStatusAndOrderDateBefore(Status status, LocalDateTime orderDate);
    
    // 자동 구매확정을 위한 메서드들
    List<Order> findByStatusAndDeliveredAtBeforeAndConfirmedFalse(Status status, LocalDateTime deliveredAt);
    long countByStatusAndDeliveredAtBeforeAndConfirmedFalse(Status status, LocalDateTime deliveredAt);
}
