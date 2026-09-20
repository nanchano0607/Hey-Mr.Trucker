package com.example.capshop.controller.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.capshop.domain.order.Order;
import com.example.capshop.domain.order.OrderItem;
import com.example.capshop.domain.order.Payment;
import com.example.capshop.domain.order.Status;
import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.user.User;
import com.example.capshop.dto.order.TossPaymentInfo;
import com.example.capshop.repository.order.OrderRepository;
import com.example.capshop.repository.order.PaymentRepository;
import com.example.capshop.repository.product.ProductRepository;
import com.example.capshop.service.common.SolapiSmsService;
import com.example.capshop.service.order.TossApiUnavailableException;
import com.example.capshop.service.order.TossPaymentClient;
import com.example.capshop.support.ApiTestSupport;

/**
 * 토스 웹훅은 인증이 없는 공개 엔드포인트이므로 본문을 믿지 않는다.
 * 입금 완료 처리 전에 서버가 토스에 직접 결제를 조회해 DONE 상태와 금액·주문번호가 맞는지 확인한다.
 */
class TossWebhookVerificationApiTest extends ApiTestSupport {

    private static final long PAYMENT_AMOUNT = 63_500L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private TossPaymentClient tossPaymentClient;
    @MockitoBean
    private SolapiSmsService solapiSmsService; // 실제 SMS 발송 차단

    @BeforeEach
    void resetMocks() {
        reset(tossPaymentClient);
    }

    @Test
    @DisplayName("토스에서 입금 완료(DONE)와 금액·주문번호가 확인되면 주문이 확정된다")
    void webhook_confirmsOrderWhenTossReportsDeposit() throws Exception {
        // Arrange
        Order order = savePendingVirtualAccountOrder();
        when(tossPaymentClient.findByPaymentKey(anyString()))
                .thenReturn(Optional.of(new TossPaymentInfo("DONE", order.getOrderId(), PAYMENT_AMOUNT)));

        // Act
        sendDoneWebhook(order.getOrderId()).andExpect(status().isOk());

        // Assert
        assertThat(statusOf(order)).isEqualTo(Status.ORDERED);
    }

    @Test
    @DisplayName("위조 웹훅: 토스에서는 아직 입금 대기 상태이면 주문을 확정하지 않는다")
    void webhook_ignoresForgedRequestWhenTossStillWaitingForDeposit() throws Exception {
        // Arrange
        Order order = savePendingVirtualAccountOrder();
        when(tossPaymentClient.findByPaymentKey(anyString()))
                .thenReturn(Optional.of(new TossPaymentInfo("WAITING_FOR_DEPOSIT", order.getOrderId(), PAYMENT_AMOUNT)));

        // Act
        sendDoneWebhook(order.getOrderId()).andExpect(status().isOk());

        // Assert
        assertThat(statusOf(order)).isEqualTo(Status.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("토스에서 확인한 금액이 주문 결제 금액과 다르면 확정하지 않는다")
    void webhook_ignoresWhenTossAmountDiffers() throws Exception {
        // Arrange
        Order order = savePendingVirtualAccountOrder();
        when(tossPaymentClient.findByPaymentKey(anyString()))
                .thenReturn(Optional.of(new TossPaymentInfo("DONE", order.getOrderId(), PAYMENT_AMOUNT - 1_000L)));

        // Act
        sendDoneWebhook(order.getOrderId()).andExpect(status().isOk());

        // Assert
        assertThat(statusOf(order)).isEqualTo(Status.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("토스가 알려준 주문번호가 다르면 확정하지 않는다")
    void webhook_ignoresWhenTossOrderIdDiffers() throws Exception {
        // Arrange
        Order order = savePendingVirtualAccountOrder();
        when(tossPaymentClient.findByPaymentKey(anyString()))
                .thenReturn(Optional.of(new TossPaymentInfo("DONE", "ORD-OTHER", PAYMENT_AMOUNT)));

        // Act
        sendDoneWebhook(order.getOrderId()).andExpect(status().isOk());

        // Assert
        assertThat(statusOf(order)).isEqualTo(Status.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("토스에 해당 결제가 없으면 확정하지 않는다")
    void webhook_ignoresWhenTossHasNoSuchPayment() throws Exception {
        // Arrange
        Order order = savePendingVirtualAccountOrder();
        when(tossPaymentClient.findByPaymentKey(anyString())).thenReturn(Optional.empty());

        // Act
        sendDoneWebhook(order.getOrderId()).andExpect(status().isOk());

        // Assert
        assertThat(statusOf(order)).isEqualTo(Status.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("토스 조회에 실패하면 확정하지 않고 토스가 재시도하도록 503으로 응답한다")
    void webhook_returnsServiceUnavailableWhenTossCannotBeReached() throws Exception {
        // Arrange
        Order order = savePendingVirtualAccountOrder();
        when(tossPaymentClient.findByPaymentKey(anyString()))
                .thenThrow(new TossApiUnavailableException("토스 결제 조회에 실패했습니다.", null));

        // Act
        sendDoneWebhook(order.getOrderId()).andExpect(status().isServiceUnavailable());

        // Assert
        assertThat(statusOf(order)).isEqualTo(Status.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("이미 처리된 주문의 중복 웹훅은 토스를 다시 조회하지 않고 그대로 넘어간다")
    void webhook_duplicateForAlreadyConfirmedOrderDoesNotCallToss() throws Exception {
        // Arrange
        Order order = savePendingVirtualAccountOrder();
        when(tossPaymentClient.findByPaymentKey(anyString()))
                .thenReturn(Optional.of(new TossPaymentInfo("DONE", order.getOrderId(), PAYMENT_AMOUNT)));
        sendDoneWebhook(order.getOrderId()).andExpect(status().isOk());
        reset(tossPaymentClient);

        // Act
        sendDoneWebhook(order.getOrderId()).andExpect(status().isOk());

        // Assert
        verify(tossPaymentClient, never()).findByPaymentKey(anyString());
        assertThat(statusOf(order)).isEqualTo(Status.ORDERED);
    }

    @Test
    @DisplayName("입금 완료(DONE)가 아닌 웹훅은 토스를 조회하지 않고 무시한다")
    void webhook_nonDoneStatusIsIgnoredWithoutCallingToss() throws Exception {
        // Arrange
        Order order = savePendingVirtualAccountOrder();

        // Act
        mockMvc.perform(post("/api/toss/webhook").with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"%s\",\"status\":\"CANCELED\"}".formatted(order.getOrderId())))
                .andExpect(status().isOk());

        // Assert
        verify(tossPaymentClient, never()).findByPaymentKey(anyString());
        assertThat(statusOf(order)).isEqualTo(Status.PAYMENT_PENDING);
    }

    private org.springframework.test.web.servlet.ResultActions sendDoneWebhook(String orderId) throws Exception {
        return mockMvc.perform(post("/api/toss/webhook").with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"%s\",\"status\":\"DONE\",\"transactionKey\":\"tx\"}".formatted(orderId)));
    }

    private Status statusOf(Order order) {
        return orderRepository.findByOrderId(order.getOrderId()).orElseThrow().getStatus();
    }

    private Order savePendingVirtualAccountOrder() {
        User buyer = saveUser("webhook-verify", false);
        Product product = new Product();
        product.setName("웹훅 검증 상품 " + System.nanoTime());
        product.setPrice(60_000L);
        product.setStock(10L);
        product = productRepository.save(product);

        Order order = new Order(buyer);
        order.setOrderId("ORD-WH-" + System.nanoTime());
        order.setStatus(Status.PAYMENT_PENDING);
        order.setVirtualAccount(true);
        order.setReceiverName("수령인");
        order.setAddress("서울");
        order.setPhone("010-0000-0000");
        order.addOrderItem(new OrderItem(product, 1, 60_000L));
        order.setOriginal_price(60_000L);
        order.setTotal_price(PAYMENT_AMOUNT);
        order.setFinal_price(PAYMENT_AMOUNT);
        order = orderRepository.save(order);
        paymentRepository.save(new Payment(order, "pk_wh_" + System.nanoTime(), "VIRTUAL_ACCOUNT", PAYMENT_AMOUNT));
        return order;
    }
}
