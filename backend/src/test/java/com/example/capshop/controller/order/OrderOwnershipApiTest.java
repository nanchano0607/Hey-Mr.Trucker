package com.example.capshop.controller.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.example.capshop.domain.order.Order;
import com.example.capshop.domain.order.OrderItem;
import com.example.capshop.domain.order.Status;
import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.order.OrderRepository;
import com.example.capshop.repository.product.ProductRepository;
import com.example.capshop.support.ApiTestSupport;

/** 주문 조회·취소·반품취소는 주문의 소유자만 할 수 있다. 타인의 주문은 존재하지 않는 것처럼(404) 응답한다. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderOwnershipApiTest extends ApiTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;

    private User owner;
    private String ownerToken;
    private String otherToken;
    private String adminToken;
    private Product product;

    @BeforeAll
    void setUpAccounts() {
        owner = saveUser("order-owner", false);
        ownerToken = bearer(owner);
        otherToken = bearer(saveUser("order-other", false));
        adminToken = bearer(saveUser("order-admin", true));

        Product newProduct = new Product();
        newProduct.setName("소유권 테스트 캡");
        newProduct.setPrice(10000L);
        product = productRepository.save(newProduct);
    }

    @Test
    @DisplayName("주문 상세는 주문의 소유자가 조회하면 200으로 내용을 돌려준다")
    void getOrderDetail_ownerCanView() throws Exception {
        // Arrange
        Order order = saveOrder(owner, Status.ORDERED);

        // Act & Assert
        mockMvc.perform(get("/api/orders/{id}", order.getId())
                .header(HttpHeaders.AUTHORIZATION, ownerToken)
                .with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getOrderId()));
    }

    @Test
    @DisplayName("주문 상세를 다른 사용자가 조회하면 존재하지 않는 주문처럼 404를 받는다")
    void getOrderDetail_otherUserGetsNotFound() throws Exception {
        // Arrange
        Order order = saveOrder(owner, Status.ORDERED);

        // Act & Assert
        String body = mockMvc.perform(get("/api/orders/{id}", order.getId())
                .header(HttpHeaders.AUTHORIZATION, otherToken)
                .with(asDefaultServlet()))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).doesNotContain(order.getOrderId()).doesNotContain(order.getPhone());
    }

    @Test
    @DisplayName("다른 사용자는 주문 상태와 관계없이 취소를 시도해도 404를 받는다 (소유자 검사가 상태 검사보다 먼저)")
    void cancelOrder_otherUserGetsNotFoundBeforeStatusValidation() throws Exception {
        // Arrange - 배송 완료 주문은 소유자가 취소하면 400(취소 불가)이다.
        Order order = saveOrder(owner, Status.DELIVERED);

        // Act & Assert
        mockMvc.perform(post("/api/orders/{id}/cancel", order.getId())
                .header(HttpHeaders.AUTHORIZATION, otherToken)
                .with(asDefaultServlet()))
                .andExpect(status().isNotFound());
        assertThat(statusOf(order)).isEqualTo(Status.DELIVERED);
    }

    @Test
    @DisplayName("소유자가 취소하면 소유자 검사를 통과해 비즈니스 검증(취소 불가 상태 400)까지 도달한다")
    void cancelOrder_ownerReachesBusinessValidation() throws Exception {
        // Arrange
        Order order = saveOrder(owner, Status.DELIVERED);

        // Act
        String body = mockMvc.perform(post("/api/orders/{id}/cancel", order.getId())
                .header(HttpHeaders.AUTHORIZATION, ownerToken)
                .with(asDefaultServlet()))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(body).contains("취소할 수 없는 주문입니다.");
    }

    @Test
    @DisplayName("다른 사용자는 남의 반품 요청을 취소할 수 없고 주문 상태는 그대로다")
    void cancelReturn_otherUserGetsNotFoundAndStatusUnchanged() throws Exception {
        // Arrange
        Order order = saveOrder(owner, Status.RETURN_REQUESTED);

        // Act
        mockMvc.perform(post("/api/orders/{id}/cancel-return", order.getId())
                .header(HttpHeaders.AUTHORIZATION, otherToken)
                .with(asDefaultServlet()))
                .andExpect(status().isNotFound());

        // Assert
        assertThat(statusOf(order)).isEqualTo(Status.RETURN_REQUESTED);
    }

    @Test
    @DisplayName("소유자는 자신의 반품 요청을 취소할 수 있다")
    void cancelReturn_ownerCanCancel() throws Exception {
        // Arrange
        Order order = saveOrder(owner, Status.RETURN_REQUESTED);

        // Act
        mockMvc.perform(post("/api/orders/{id}/cancel-return", order.getId())
                .header(HttpHeaders.AUTHORIZATION, ownerToken)
                .with(asDefaultServlet()))
                .andExpect(status().isOk());

        // Assert
        assertThat(statusOf(order)).isEqualTo(Status.DELIVERED);
    }

    @Test
    @DisplayName("관리자용 반품 취소(/api/admin/orders/{id}/cancel-return)는 소유자와 무관하게 동작한다")
    void adminCancelReturn_worksRegardlessOfOwner() throws Exception {
        // Arrange
        Order order = saveOrder(owner, Status.RETURN_REQUESTED);

        // Act
        mockMvc.perform(post("/api/admin/orders/{id}/cancel-return", order.getId())
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet()))
                .andExpect(status().isOk());

        // Assert
        assertThat(statusOf(order)).isEqualTo(Status.DELIVERED);
    }

    private Order saveOrder(User orderer, Status status) {
        Order order = new Order(orderer);
        order.setOrderId("ORD-OWN-" + System.nanoTime());
        order.setStatus(status);
        order.setReceiverName("수령인");
        order.setAddress("서울시 테스트구");
        order.setPhone("010-5555-6666");
        order.addOrderItem(new OrderItem(product, 1, 10000L, "FREE"));
        order.calculateTotalPrice();
        return orderRepository.save(order);
    }

    private Status statusOf(Order order) {
        return orderRepository.findById(order.getId()).orElseThrow().getStatus();
    }
}
