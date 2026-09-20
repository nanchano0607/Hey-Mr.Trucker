package com.example.capshop.controller.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.capshop.domain.coupon.Coupon;
import com.example.capshop.domain.coupon.CouponType;
import com.example.capshop.domain.coupon.UserCoupon;
import com.example.capshop.domain.order.CheckOut;
import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.coupon.CouponRepository;
import com.example.capshop.repository.coupon.UserCouponRepository;
import com.example.capshop.repository.order.CheckOutRepository;
import com.example.capshop.repository.order.OrderRepository;
import com.example.capshop.repository.product.ProductRepository;
import com.example.capshop.support.ApiTestSupport;

/**
 * 결제 승인 시 서버가 계산한 결제 금액과 요청 금액이 다르면 토스 승인 호출 전에 거부한다.
 * 실수로 외부 결제 API 가 호출되지 않도록 토스 시크릿 키를 비운다. (키가 없으면 토스 호출 단계에서 실패하므로
 * "금액 검증을 통과했는가"를 오류 메시지로 구분할 수 있다)
 */
@TestPropertySource(properties = "toss.payments.secret-key=")
class PaymentAmountVerificationApiTest extends ApiTestSupport {

    private static final String AMOUNT_MISMATCH = "결제 금액이 일치하지 않습니다.";
    private static final String REACHED_TOSS_STEP = "토스 시크릿 키가 설정되지 않았습니다";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CheckOutRepository checkOutRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private UserCouponRepository userCouponRepository;

    @Test
    @DisplayName("상품 10만원을 100원만 결제했다고 요청하면 토스 승인 전에 거부하고 주문을 만들지 않는다")
    void confirm_rejectsTamperedAmount() throws Exception {
        // Arrange
        User user = saveUser("amount-tamper", false);
        CheckOut checkOut = saveCheckOut(user, saveProduct(100_000L));

        // Act
        String body = confirm(user, checkOut, 100L, null);

        // Assert
        assertThat(body).contains(AMOUNT_MISMATCH).doesNotContain(REACHED_TOSS_STEP);
        assertThat(orderRepository.findByOrderId(checkOut.getOrderId())).isEmpty();
    }

    @Test
    @DisplayName("서버 계산 금액과 같은 금액이면 금액 검증을 통과해 토스 승인 단계까지 진행한다")
    void confirm_correctAmountPassesAmountCheck() throws Exception {
        // Arrange
        User user = saveUser("amount-ok", false);
        CheckOut checkOut = saveCheckOut(user, saveProduct(100_000L));

        // Act
        String body = confirm(user, checkOut, 100_000L, null);

        // Assert
        assertThat(body).contains(REACHED_TOSS_STEP).doesNotContain(AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("7만원 미만 상품은 배송비를 뺀 금액으로 요청하면 거부한다")
    void confirm_rejectsAmountMissingShippingFee() throws Exception {
        // Arrange
        User user = saveUser("amount-shipping", false);
        CheckOut checkOut = saveCheckOut(user, saveProduct(30_000L));

        // Act & Assert
        assertThat(confirm(user, checkOut, 30_000L, null)).contains(AMOUNT_MISMATCH);
        assertThat(confirm(user, checkOut, 33_500L, null)).contains(REACHED_TOSS_STEP);
    }

    @Test
    @DisplayName("클라이언트가 보낸 할인 금액(finalAmount, couponDiscount)을 믿지 않고 서버가 쿠폰 할인을 계산한다")
    void confirm_ignoresClientSuppliedDiscountAmounts() throws Exception {
        // Arrange
        User user = saveUser("amount-discount", false);
        CheckOut checkOut = saveCheckOut(user, saveProduct(80_000L));
        UserCoupon coupon = saveUserCoupon(user, CouponType.PERCENTAGE, 10, 5_000L);
        String forgedInfo = "{\"userCouponId\":%d,\"couponDiscount\":80000,\"originalAmount\":80000,\"finalAmount\":1}"
                .formatted(coupon.getId());
        String honestInfo = "{\"userCouponId\":%d}".formatted(coupon.getId());

        // Act & Assert - 실제 할인은 최대 5,000원이므로 정상 금액은 75,000원
        assertThat(confirm(user, checkOut, 1L, forgedInfo)).contains(AMOUNT_MISMATCH);
        assertThat(confirm(user, checkOut, 75_000L, honestInfo)).contains(REACHED_TOSS_STEP);
        assertThat(confirm(user, checkOut, 80_000L, honestInfo)).contains(AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("보유하지 않은 포인트를 쓰겠다고 하면 토스 승인 전에 거부한다")
    void confirm_rejectsPointsOverBalance() throws Exception {
        // Arrange
        User user = saveUser("amount-points", false);
        CheckOut checkOut = saveCheckOut(user, saveProduct(80_000L));

        // Act
        String body = confirm(user, checkOut, 1L, "{\"pointsUsed\":79999}");

        // Assert
        assertThat(body).doesNotContain(REACHED_TOSS_STEP);
        assertThat(orderRepository.findByOrderId(checkOut.getOrderId())).isEmpty();
    }

    @Test
    @DisplayName("수량이 음수인 주문 상품으로 금액을 줄이려 해도 거부한다")
    void confirm_rejectsNegativeQuantityItems() throws Exception {
        // Arrange
        User user = saveUser("amount-negative", false);
        Product expensive = saveProduct(100_000L);
        Product cheap = saveProduct(10_000L);
        String items = "[{\"productId\":%d,\"quantity\":1},{\"productId\":%d,\"quantity\":-5}]"
                .formatted(expensive.getId(), cheap.getId());
        CheckOut checkOut = checkOutRepository.save(newCheckOut(user, items));

        // Act
        String body = confirm(user, checkOut, 50_000L, null);

        // Assert
        assertThat(body).doesNotContain(REACHED_TOSS_STEP);
        assertThat(orderRepository.findByOrderId(checkOut.getOrderId())).isEmpty();
    }

    private String confirm(User user, CheckOut checkOut, long amount, String discountInfoJson) throws Exception {
        String discountPart = discountInfoJson == null ? "" : ",\"discountInfo\":" + discountInfoJson;
        return mockMvc.perform(post("/api/orders/confirm")
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentKey\":\"pk_test_x\",\"orderId\":\"%s\",\"amount\":%d%s}"
                        .formatted(checkOut.getOrderId(), amount, discountPart)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Product saveProduct(long price) {
        Product product = new Product();
        product.setName("결제 금액 검증 상품 " + System.nanoTime());
        product.setPrice(price);
        return productRepository.save(product);
    }

    private CheckOut saveCheckOut(User owner, Product product) {
        String items = "[{\"productId\":%d,\"quantity\":1,\"size\":\"FREE\"}]".formatted(product.getId());
        return checkOutRepository.save(newCheckOut(owner, items));
    }

    private CheckOut newCheckOut(User owner, String itemsJson) {
        CheckOut checkOut = new CheckOut("수령인", "서울시 테스트구", "010-1111-2222", itemsJson);
        checkOut.setUserId(owner.getId());
        checkOut.setOrderId("ORD-AMT-" + System.nanoTime());
        return checkOut;
    }

    private UserCoupon saveUserCoupon(User owner, CouponType type, int value, Long maxDiscount) {
        Coupon coupon = couponRepository.save(new Coupon(
                "금액 검증 쿠폰 " + System.nanoTime(), "AMT" + System.nanoTime(), type, value, null, maxDiscount, "테스트"));
        return userCouponRepository.save(new UserCoupon(owner, coupon));
    }
}
