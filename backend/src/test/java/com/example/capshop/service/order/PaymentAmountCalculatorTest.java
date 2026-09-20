package com.example.capshop.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.capshop.domain.coupon.Coupon;
import com.example.capshop.domain.coupon.CouponStatus;
import com.example.capshop.domain.coupon.CouponType;
import com.example.capshop.domain.coupon.UserCoupon;
import com.example.capshop.domain.order.CheckOut;
import com.example.capshop.domain.order.PaymentBreakdown;
import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.user.User;
import com.example.capshop.dto.order.DiscountSelection;
import com.example.capshop.repository.coupon.CouponRepository;
import com.example.capshop.repository.coupon.UserCouponRepository;
import com.example.capshop.repository.product.ProductRepository;
import com.example.capshop.repository.user.UserRepository;
import com.example.capshop.support.ApiTestSupport;

/** 서버가 DB 의 상품 가격·쿠폰·포인트 잔액으로 결제 금액을 직접 계산한다. 클라이언트가 보낸 금액은 쓰지 않는다. */
class PaymentAmountCalculatorTest extends ApiTestSupport {

    @Autowired
    private PaymentAmountCalculator calculator;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private UserCouponRepository userCouponRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("할인이 없고 상품금액이 7만원 미만이면 상품금액에 배송비 3,500원이 붙는다")
    void calculate_addsShippingBelowThreshold() {
        // Arrange
        User user = saveUser("calc-basic", false);
        Product product = saveProduct(30_000L);
        CheckOut checkOut = checkOut(user, items(product, 2));

        // Act
        PaymentBreakdown result = calculator.calculate(user, checkOut, DiscountSelection.none());

        // Assert
        assertThat(result.productAmount()).isEqualTo(60_000L);
        assertThat(result.shippingFee()).isEqualTo(3_500L);
        assertThat(result.payableAmount()).isEqualTo(63_500L);
    }

    @Test
    @DisplayName("상품금액이 정확히 7만원이면 무료배송이다")
    void calculate_freeShippingAtThreshold() {
        // Arrange
        User user = saveUser("calc-free", false);
        Product product = saveProduct(35_000L);
        CheckOut checkOut = checkOut(user, items(product, 2));

        // Act
        PaymentBreakdown result = calculator.calculate(user, checkOut, DiscountSelection.none());

        // Assert
        assertThat(result.shippingFee()).isZero();
        assertThat(result.payableAmount()).isEqualTo(70_000L);
    }

    @Test
    @DisplayName("퍼센트 쿠폰은 최대 할인금액을 넘지 않고, 배송비는 할인 전 상품금액으로 판단한다")
    void calculate_percentageCouponWithCap() {
        // Arrange
        User user = saveUser("calc-pct", false);
        Product product = saveProduct(80_000L);
        UserCoupon coupon = saveUserCoupon(user, CouponType.PERCENTAGE, 10, null, 5_000L);
        CheckOut checkOut = checkOut(user, items(product, 1));

        // Act
        PaymentBreakdown result = calculator.calculate(user, checkOut, new DiscountSelection(coupon.getId(), 0L));

        // Assert
        assertThat(result.couponDiscount()).isEqualTo(5_000L);
        assertThat(result.shippingFee()).isZero();
        assertThat(result.payableAmount()).isEqualTo(75_000L);
    }

    @Test
    @DisplayName("퍼센트 쿠폰은 원 단위 이하를 버린다")
    void calculate_percentageCouponFloorsToWon() {
        // Arrange
        User user = saveUser("calc-floor", false);
        Product product = saveProduct(19_999L);
        UserCoupon coupon = saveUserCoupon(user, CouponType.PERCENTAGE, 15, null, null);
        CheckOut checkOut = checkOut(user, items(product, 1));

        // Act
        PaymentBreakdown result = calculator.calculate(user, checkOut, new DiscountSelection(coupon.getId(), 0L));

        // Assert - 19,999 * 15 / 100 = 2,999.85 → 2,999
        assertThat(result.couponDiscount()).isEqualTo(2_999L);
    }

    @Test
    @DisplayName("정액 쿠폰과 포인트를 함께 쓰면 둘 다 차감되고 배송비가 더해진다")
    void calculate_amountCouponAndPoints() {
        // Arrange
        User user = saveUserWithPoints("calc-both", 5_000L);
        Product product = saveProduct(60_000L);
        UserCoupon coupon = saveUserCoupon(user, CouponType.AMOUNT, 3_000, null, null);
        CheckOut checkOut = checkOut(user, items(product, 1));

        // Act
        PaymentBreakdown result = calculator.calculate(user, checkOut, new DiscountSelection(coupon.getId(), 2_000L));

        // Assert - 60,000 - 3,000 - 2,000 + 3,500
        assertThat(result.payableAmount()).isEqualTo(58_500L);
    }

    @Test
    @DisplayName("보유 포인트보다 많은 포인트를 쓰겠다고 하면 거부한다")
    void calculate_rejectsPointsOverBalance() {
        // Arrange
        User user = saveUserWithPoints("calc-points-balance", 1_000L);
        CheckOut checkOut = checkOut(user, items(saveProduct(50_000L), 1));

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculate(user, checkOut, new DiscountSelection(null, 1_001L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("적립금");
    }

    @Test
    @DisplayName("쿠폰 적용 후 상품금액보다 많은 포인트를 쓰겠다고 하면 거부한다")
    void calculate_rejectsPointsOverPayableProductAmount() {
        // Arrange
        User user = saveUserWithPoints("calc-points-cap", 100_000L);
        UserCoupon coupon = saveUserCoupon(user, CouponType.AMOUNT, 3_000, null, null);
        CheckOut checkOut = checkOut(user, items(saveProduct(10_000L), 1));

        // Act & Assert - 쿠폰 후 남는 상품금액은 7,000원
        assertThatThrownBy(() -> calculator.calculate(user, checkOut, new DiscountSelection(coupon.getId(), 7_001L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("음수 포인트는 거부한다")
    void calculate_rejectsNegativePoints() {
        // Arrange
        User user = saveUserWithPoints("calc-points-neg", 5_000L);
        CheckOut checkOut = checkOut(user, items(saveProduct(50_000L), 1));

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculate(user, checkOut, new DiscountSelection(null, -1_000L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("남의 쿠폰을 쓰겠다고 하면 거부한다")
    void calculate_rejectsOtherUsersCoupon() {
        // Arrange
        User owner = saveUser("calc-coupon-owner", false);
        User user = saveUser("calc-coupon-thief", false);
        UserCoupon othersCoupon = saveUserCoupon(owner, CouponType.AMOUNT, 3_000, null, null);
        CheckOut checkOut = checkOut(user, items(saveProduct(50_000L), 1));

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculate(user, checkOut, new DiscountSelection(othersCoupon.getId(), 0L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("본인 소유");
    }

    @Test
    @DisplayName("이미 사용했거나 만료된 쿠폰은 거부한다")
    void calculate_rejectsUsedOrExpiredCoupon() {
        // Arrange
        User user = saveUser("calc-coupon-state", false);
        CheckOut checkOut = checkOut(user, items(saveProduct(50_000L), 1));
        UserCoupon used = saveUserCoupon(user, CouponType.AMOUNT, 3_000, null, null);
        used.setStatus(CouponStatus.USED);
        userCouponRepository.save(used);
        UserCoupon expired = saveUserCoupon(user, CouponType.AMOUNT, 3_000, null, null);
        expired.setValidUntil(LocalDateTime.now().minusDays(1));
        userCouponRepository.save(expired);

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculate(user, checkOut, new DiscountSelection(used.getId(), 0L)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calculator.calculate(user, checkOut, new DiscountSelection(expired.getId(), 0L)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("최소 주문금액에 못 미치는 쿠폰은 거부한다")
    void calculate_rejectsCouponBelowMinimumOrderAmount() {
        // Arrange
        User user = saveUser("calc-coupon-min", false);
        UserCoupon coupon = saveUserCoupon(user, CouponType.AMOUNT, 3_000, 100_000L, null);
        CheckOut checkOut = checkOut(user, items(saveProduct(50_000L), 1));

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculate(user, checkOut, new DiscountSelection(coupon.getId(), 0L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰은 거부한다")
    void calculate_rejectsUnknownCoupon() {
        // Arrange
        User user = saveUser("calc-coupon-none", false);
        CheckOut checkOut = checkOut(user, items(saveProduct(50_000L), 1));

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculate(user, checkOut, new DiscountSelection(999_999_999L, 0L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("수량이 0 이하이거나 없는 상품, 잘못된 JSON, 빈 주문은 거부한다")
    void calculate_rejectsMalformedItems() {
        // Arrange
        User user = saveUser("calc-items", false);
        Product product = saveProduct(50_000L);
        String zeroQuantity = "[{\"productId\":%d,\"quantity\":0}]".formatted(product.getId());
        String negativeQuantity = "[{\"productId\":%d,\"quantity\":-3}]".formatted(product.getId());
        String unknownProduct = "[{\"productId\":999999999,\"quantity\":1}]";

        // Act & Assert
        for (String itemsJson : new String[] {zeroQuantity, negativeQuantity, unknownProduct, "not-json", "[]", "{}"}) {
            CheckOut checkOut = checkOut(user, itemsJson);
            assertThatThrownBy(() -> calculator.calculate(user, checkOut, DiscountSelection.none()))
                    .as(itemsJson)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private Product saveProduct(long price) {
        Product product = new Product();
        product.setName("금액 계산 테스트 상품 " + System.nanoTime());
        product.setPrice(price);
        return productRepository.save(product);
    }

    private String items(Product product, int quantity) {
        return "[{\"productId\":%d,\"quantity\":%d,\"size\":\"FREE\"}]".formatted(product.getId(), quantity);
    }

    private CheckOut checkOut(User owner, String itemsJson) {
        CheckOut checkOut = new CheckOut("수령인", "서울시 테스트구", "010-1111-2222", itemsJson);
        checkOut.setUserId(owner.getId());
        return checkOut;
    }

    private User saveUserWithPoints(String prefix, long points) {
        User user = saveUser(prefix, false);
        user.addPoints(points);
        return userRepository.save(user);
    }

    private UserCoupon saveUserCoupon(User owner, CouponType type, int value, Long minOrderAmount, Long maxDiscount) {
        Coupon coupon = couponRepository.save(new Coupon(
                "테스트 쿠폰 " + System.nanoTime(), "CODE" + System.nanoTime(), type, value,
                minOrderAmount, maxDiscount, "금액 계산 테스트"));
        return userCouponRepository.save(new UserCoupon(owner, coupon));
    }
}
