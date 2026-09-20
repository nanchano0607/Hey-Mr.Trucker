package com.example.capshop.controller.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.capshop.domain.order.CheckOut;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.order.CheckOutRepository;
import com.example.capshop.support.ApiTestSupport;

/** 체크아웃은 만든 사람만 조회할 수 있고, 저장할 때 클라이언트가 보낸 id/userId 는 무시한다. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckOutOwnershipApiTest extends ApiTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CheckOutRepository checkOutRepository;

    private User owner;
    private User other;
    private String ownerToken;
    private String otherToken;

    @BeforeAll
    void setUpAccounts() {
        owner = saveUser("checkout-owner", false);
        other = saveUser("checkout-other", false);
        ownerToken = bearer(owner);
        otherToken = bearer(other);
    }

    @Test
    @DisplayName("체크아웃은 만든 사용자가 조회하면 200으로 돌려준다")
    void getCheckout_ownerCanView() throws Exception {
        // Arrange
        CheckOut checkOut = saveCheckOut(owner, "원본 수령인");

        // Act & Assert
        mockMvc.perform(get("/api/checkout/{id}", checkOut.getId())
                .header(HttpHeaders.AUTHORIZATION, ownerToken)
                .with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(checkOut.getOrderId()));
    }

    @Test
    @DisplayName("다른 사용자가 남의 체크아웃을 조회하면 404를 받는다")
    void getCheckout_otherUserGetsNotFound() throws Exception {
        // Arrange
        CheckOut checkOut = saveCheckOut(owner, "원본 수령인");

        // Act & Assert
        mockMvc.perform(get("/api/checkout/{id}", checkOut.getId())
                .header(HttpHeaders.AUTHORIZATION, otherToken)
                .with(asDefaultServlet()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("체크아웃 저장 시 본문의 id·userId 는 무시하고 남의 체크아웃을 덮어쓰지 않는다")
    void createCheckout_ignoresClientSuppliedIdAndUserId() throws Exception {
        // Arrange
        CheckOut victim = saveCheckOut(owner, "원본 수령인");
        String forgedBody = """
                {"id":%d,"userId":%d,"orderId":"ORD-FORGED",
                 "name":"변조된 수령인","address":"변조 주소","phone":"010-0000-0000",
                 "itemsJson":"[{\\"productId\\":1,\\"quantity\\":1}]"}
                """.formatted(victim.getId(), owner.getId());

        // Act
        mockMvc.perform(post("/api/checkout")
                .header(HttpHeaders.AUTHORIZATION, otherToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgedBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(other.getId()));

        // Assert - 기존 체크아웃은 그대로
        CheckOut untouched = checkOutRepository.findById(victim.getId()).orElseThrow();
        assertThat(untouched.getName()).isEqualTo("원본 수령인");
        assertThat(untouched.getUserId()).isEqualTo(owner.getId());
        assertThat(untouched.getOrderId()).isEqualTo(victim.getOrderId());
    }

    private CheckOut saveCheckOut(User orderer, String receiver) {
        CheckOut checkOut = new CheckOut(receiver, "서울시 테스트구", "010-1111-2222",
                "[{\"productId\":1,\"quantity\":1}]");
        checkOut.setUserId(orderer.getId());
        checkOut.setOrderId("ORD-CHK-" + System.nanoTime());
        return checkOutRepository.save(checkOut);
    }
}
