package com.example.capshop.domain.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShippingPolicyTest {

    @ParameterizedTest(name = "상품금액 {0}원 → 배송비 {1}원")
    @CsvSource({"0,3500", "1,3500", "30000,3500", "69999,3500", "70000,0", "70001,0", "500000,0"})
    @DisplayName("상품금액(배송비·할인 제외)이 70,000원 이상이면 무료, 미만이면 3,500원이다")
    void feeFor_appliesFreeShippingThreshold(long productAmount, long expectedFee) {
        // Arrange & Act
        long fee = ShippingPolicy.feeFor(productAmount);

        // Assert
        assertThat(fee).isEqualTo(expectedFee);
    }

    @Test
    @DisplayName("정책 상수는 프론트(PaymentPage)와 같은 값이다")
    void constants_matchFrontend() {
        // Arrange & Act & Assert
        assertThat(ShippingPolicy.FEE).isEqualTo(3_500L);
        assertThat(ShippingPolicy.FREE_THRESHOLD).isEqualTo(70_000L);
    }
}
