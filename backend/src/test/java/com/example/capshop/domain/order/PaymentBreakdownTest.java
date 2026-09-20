package com.example.capshop.domain.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentBreakdownTest {

    @Test
    @DisplayName("결제 금액은 상품금액에서 쿠폰·포인트를 뺀 값에 배송비를 더한 값이다")
    void payableAmount_subtractsDiscountsAndAddsShipping() {
        // Arrange
        PaymentBreakdown breakdown = new PaymentBreakdown(60_000L, 6_000L, 1_000L, 3_500L);

        // Act & Assert
        assertThat(breakdown.payableAmount()).isEqualTo(56_500L);
    }

    @Test
    @DisplayName("할인이 상품금액보다 커도 상품 부분은 0원 아래로 내려가지 않고 배송비만 남는다")
    void payableAmount_neverGoesBelowShippingFee() {
        // Arrange
        PaymentBreakdown breakdown = new PaymentBreakdown(10_000L, 8_000L, 5_000L, 3_500L);

        // Act & Assert
        assertThat(breakdown.payableAmount()).isEqualTo(3_500L);
    }

    @Test
    @DisplayName("무료배송이고 할인이 없으면 상품금액 그대로다")
    void payableAmount_withoutDiscountAndShipping() {
        // Arrange
        PaymentBreakdown breakdown = new PaymentBreakdown(80_000L, 0L, 0L, 0L);

        // Act & Assert
        assertThat(breakdown.payableAmount()).isEqualTo(80_000L);
    }
}
