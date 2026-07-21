package com.example.capshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.capshop.domain.order.Order;
import com.example.capshop.domain.order.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder(Order order);
    Optional<Payment> findByPaymentKey(String paymentKey);
    Optional<Payment> findTopByOrderOrderByIdDesc(Order order);
}
