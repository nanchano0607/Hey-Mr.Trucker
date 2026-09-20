package com.example.capshop.dto.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscountSelectionTest {

    @Test
    @DisplayName("할인 정보가 없으면 쿠폰도 포인트도 사용하지 않는다")
    void from_nullMeansNoDiscount() {
        // Arrange & Act
        DiscountSelection selection = DiscountSelection.from(null);

        // Assert
        assertThat(selection.userCouponId()).isNull();
        assertThat(selection.points()).isZero();
    }

    @Test
    @DisplayName("userCouponId 와 pointsUsed 만 읽고, 클라이언트가 보낸 금액 필드는 무시한다")
    void from_readsOnlySelectionFields() {
        // Arrange
        Map<String, Object> info = new HashMap<>();
        info.put("userCouponId", 7);
        info.put("pointsUsed", 1500L);
        info.put("finalAmount", 1);
        info.put("couponDiscount", 99_999);

        // Act
        DiscountSelection selection = DiscountSelection.from(info);

        // Assert
        assertThat(selection.userCouponId()).isEqualTo(7L);
        assertThat(selection.points()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("userCouponId 가 없으면 couponId 를 대신 사용한다 (기존 호환)")
    void from_fallsBackToCouponId() {
        // Arrange
        Map<String, Object> info = Map.of("couponId", 3);

        // Act & Assert
        assertThat(DiscountSelection.from(info).userCouponId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("숫자가 아닌 값은 거부한다")
    void from_rejectsNonNumericValues() {
        // Arrange
        Map<String, Object> info = Map.of("pointsUsed", "많이");

        // Act & Assert
        assertThatThrownBy(() -> DiscountSelection.from(info)).isInstanceOf(IllegalArgumentException.class);
    }
}
