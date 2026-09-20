package com.example.capshop.domain.order;

/** 서버가 계산한 결제 금액 구성. 결제 승인 시 클라이언트가 보낸 금액과 비교하는 기준이다. */
public record PaymentBreakdown(long productAmount, long couponDiscount, long pointsDiscount, long shippingFee) {

    /** 상품금액에서 쿠폰·포인트를 뺀 값(0 미만 불가)에 배송비를 더한 실제 결제 금액. */
    public long payableAmount() {
        return Math.max(0L, productAmount - couponDiscount - pointsDiscount) + shippingFee;
    }
}
