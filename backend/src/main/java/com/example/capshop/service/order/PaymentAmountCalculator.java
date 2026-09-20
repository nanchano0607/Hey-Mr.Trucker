package com.example.capshop.service.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.capshop.domain.coupon.UserCoupon;
import com.example.capshop.domain.order.CheckOut;
import com.example.capshop.domain.order.PaymentBreakdown;
import com.example.capshop.domain.order.ShippingPolicy;
import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.user.User;
import com.example.capshop.dto.order.DiscountSelection;
import com.example.capshop.repository.coupon.UserCouponRepository;
import com.example.capshop.repository.product.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * 결제 금액을 서버가 직접 계산한다. DB 의 상품 가격, 쿠폰(소유·유효성·최소주문금액·할인율), 포인트 잔액,
 * 배송비 정책만 사용하며 클라이언트가 보낸 금액은 신뢰하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class PaymentAmountCalculator {

    private final ProductRepository productRepository;
    private final UserCouponRepository userCouponRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PaymentBreakdown calculate(User user, CheckOut checkOut, DiscountSelection selection) {
        long productAmount = sumProductAmount(checkOut.getItemsJson());
        long couponDiscount = couponDiscount(user, selection.userCouponId(), productAmount);
        long pointsDiscount = pointsDiscount(user, selection.points(), productAmount - couponDiscount);
        return new PaymentBreakdown(productAmount, couponDiscount, pointsDiscount, ShippingPolicy.feeFor(productAmount));
    }

    private long sumProductAmount(String itemsJson) {
        JsonNode items = parseItems(itemsJson);
        if (!items.isArray() || items.isEmpty()) {
            throw new IllegalArgumentException("주문 상품 정보가 올바르지 않습니다.");
        }

        long total = 0L;
        try {
            for (JsonNode item : items) {
                long quantity = item.path("quantity").asLong(0L);
                if (!item.path("productId").canConvertToLong() || quantity < 1) {
                    throw new IllegalArgumentException("주문 상품 정보가 올바르지 않습니다.");
                }
                Product product = productRepository.findById(item.get("productId").asLong())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "상품을 찾을 수 없습니다: " + item.get("productId").asText()));
                if (product.getPrice() == null) {
                    throw new IllegalArgumentException("가격이 없는 상품입니다: " + product.getId());
                }
                total = Math.addExact(total, Math.multiplyExact(product.getPrice(), quantity));
            }
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("주문 금액이 올바르지 않습니다.", overflow);
        }
        return total;
    }

    private JsonNode parseItems(String itemsJson) {
        try {
            return objectMapper.readTree(itemsJson == null ? "" : itemsJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("주문 상품 정보가 올바르지 않습니다.", e);
        }
    }

    private long couponDiscount(User user, Long userCouponId, long productAmount) {
        if (userCouponId == null) {
            return 0L;
        }
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 쿠폰입니다."));
        if (!userCoupon.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인 소유의 쿠폰이 아닙니다.");
        }
        if (!userCoupon.isValid()) {
            throw new IllegalStateException("사용할 수 없는 쿠폰입니다.");
        }
        return Math.min(userCoupon.getCoupon().calculateDiscount(productAmount), productAmount);
    }

    private long pointsDiscount(User user, long requestedPoints, long payableProductAmount) {
        if (requestedPoints < 0) {
            throw new IllegalArgumentException("사용할 적립금은 0 이상이어야 합니다.");
        }
        if (requestedPoints == 0) {
            return 0L;
        }
        if (requestedPoints > user.getAvailablePoints()) {
            throw new IllegalArgumentException("보유 적립금이 부족합니다.");
        }
        if (requestedPoints > Math.max(0L, payableProductAmount)) {
            throw new IllegalArgumentException("상품 금액을 초과해 적립금을 사용할 수 없습니다.");
        }
        return requestedPoints;
    }
}
