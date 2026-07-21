package com.example.capshop.domain;

public enum PaymentStatus {
    READY,              // 결제 요청됨, 승인 대기
    APPROVED,           // 결제 승인 완료
    FAILED,             // 결제 실패
    CANCELED,           // 결제 취소
    REFUNDED,           // 전체 환불
    PARTIAL_REFUNDED    // 부분 환불
}
