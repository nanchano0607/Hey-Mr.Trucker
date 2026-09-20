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

import com.example.capshop.domain.order.CheckOut;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.order.CheckOutRepository;
import com.example.capshop.repository.order.OrderRepository;
import com.example.capshop.support.ApiTestSupport;

/**
 * 결제 승인은 자신의 체크아웃에 대해서만 가능하고, 소유자 검증은 토스 승인 호출보다 먼저 이뤄져야 한다.
 * 실수로 외부 결제 API 가 호출되지 않도록 토스 시크릿 키를 비운다. (키가 없으면 토스 호출 전에 실패한다)
 */
@TestPropertySource(properties = "toss.payments.secret-key=")
class PaymentConfirmOwnershipApiTest extends ApiTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CheckOutRepository checkOutRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("다른 사용자의 체크아웃으로 결제 승인을 요청하면 토스 호출 전에 거부하고 주문을 만들지 않는다")
    void confirmPayment_rejectsOtherUsersCheckoutBeforeCallingToss() throws Exception {
        // Arrange
        User owner = saveUser("confirm-owner", false);
        User other = saveUser("confirm-other", false);
        CheckOut checkOut = new CheckOut("수령인", "서울시 테스트구", "010-1111-2222",
                "[{\"productId\":1,\"quantity\":1}]");
        checkOut.setUserId(owner.getId());
        checkOut.setOrderId("ORD-CONFIRM-" + System.nanoTime());
        checkOutRepository.save(checkOut);

        // Act
        String body = mockMvc.perform(post("/api/orders/confirm")
                .header(HttpHeaders.AUTHORIZATION, bearer(other))
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentKey\":\"pk_test_x\",\"orderId\":\"%s\",\"amount\":10000}"
                        .formatted(checkOut.getOrderId())))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(body).contains("주문 정보가 일치하지 않습니다.");
        assertThat(orderRepository.findByOrderId(checkOut.getOrderId())).isEmpty();
    }
}
