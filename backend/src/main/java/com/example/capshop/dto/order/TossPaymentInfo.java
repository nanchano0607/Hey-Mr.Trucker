package com.example.capshop.dto.order;

/** 토스 결제 조회 결과 중 입금 검증에 필요한 값. */
public record TossPaymentInfo(String status, String orderId, long totalAmount) {
}
