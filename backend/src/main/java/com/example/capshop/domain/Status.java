package com.example.capshop.domain;

public enum Status {
    PAYMENT_PENDING,     // ✅ 가상계좌 입금대기 (결제 미완료)
    PAYMENT_EXPIRED,     // 가상계좌 입금기한 만료
    ORDERED,            // 상품 준비중 (취소 가능)
    PREPARING_SHIPMENT, // 배송준비중 (배송 대기)
    SHIPPED,            // 배송중 (취소 불가)
    DELIVERED,          // 배송 완료 (반품 가능)
    CANCELLED,          // 주문 취소됨
    RETURN_REQUESTED,   // 반품 요청 (관리자 승인 대기)
    RETURN_SHIPPING,    // 반품 배송중 (상품 회수중)
    RETURNED            // 반품 완료 (환불 완료)
}
