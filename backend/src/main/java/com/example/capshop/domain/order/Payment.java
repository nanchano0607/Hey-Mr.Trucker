package com.example.capshop.domain.order;

import com.example.capshop.domain.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;         


@Entity
@Getter @Setter
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    private String paymentKey;      // 토스 결제 고유키
    private String method;          // CARD, KAKAO_PAY, TOSS_PAY 등
    private Long amount;            // 결제 금액
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;   // READY, APPROVED, FAILED, CANCELED, REFUNDED
    
    private LocalDateTime requestedAt;  // 결제 요청 시각
    private LocalDateTime approvedAt;   // 승인 시각
    private LocalDateTime canceledAt;   // 취소 시각
    
    private String failReason;      // 실패 사유

    public Payment() {}

    public Payment(Order order, String paymentKey, String method, Long amount) {
        this.order = order;
        this.paymentKey = paymentKey;
        this.method = method;
        this.amount = amount;
        this.status = PaymentStatus.READY;
        this.requestedAt = LocalDateTime.now();
    }

    public void approve() {
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
        this.canceledAt = LocalDateTime.now();
    }

    public void partialRefund() {
        this.status = PaymentStatus.PARTIAL_REFUNDED;
        this.canceledAt = LocalDateTime.now();
    }
}
