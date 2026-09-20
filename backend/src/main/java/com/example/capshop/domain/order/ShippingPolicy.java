package com.example.capshop.domain.order;

/**
 * 배송비 정책. 프론트(PaymentPage)의 SHIPPING_FEE / FREE_SHIPPING_THRESHOLD 와 같은 값이어야 한다.
 * 무료배송 여부는 할인 전 상품금액(배송비 제외)으로 판단한다.
 */
public final class ShippingPolicy {

    public static final long FEE = 3_500L;
    public static final long FREE_THRESHOLD = 70_000L;

    private ShippingPolicy() {
    }

    public static long feeFor(long productAmount) {
        return productAmount >= FREE_THRESHOLD ? 0L : FEE;
    }
}
