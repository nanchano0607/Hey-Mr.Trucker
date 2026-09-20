package com.example.capshop.dto.order;

import java.util.Map;

/**
 * 클라이언트가 선택한 할인 수단(쿠폰, 사용할 포인트). 금액은 담지 않는다.
 * 실제 할인 금액은 서버가 계산하므로, 클라이언트가 보낸 금액 필드는 읽지 않는다.
 */
public record DiscountSelection(Long userCouponId, long points) {

    private static final DiscountSelection NONE = new DiscountSelection(null, 0L);

    public static DiscountSelection none() {
        return NONE;
    }

    public static DiscountSelection from(Map<String, Object> discountInfo) {
        if (discountInfo == null) {
            return NONE;
        }
        Long couponId = longOrNull(discountInfo.get("userCouponId"), "userCouponId");
        if (couponId == null) {
            couponId = longOrNull(discountInfo.get("couponId"), "couponId");
        }
        Long points = longOrNull(discountInfo.get("pointsUsed"), "pointsUsed");
        return new DiscountSelection(couponId, points == null ? 0L : points);
    }

    private static Long longOrNull(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue())) {
            return number.longValue();
        }
        throw new IllegalArgumentException("할인 정보의 " + field + " 값이 올바르지 않습니다.");
    }
}
