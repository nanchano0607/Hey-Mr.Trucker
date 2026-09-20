package com.example.capshop.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.example.capshop.dto.order.TossPaymentInfo;

class RestTemplateTossPaymentClientTest {

    private static final String SECRET_KEY = "test_sk_secret";
    private static final String PAYMENTS_URL = "https://api.tosspayments.com/v1/payments/";

    private MockRestServiceServer server;
    private RestTemplateTossPaymentClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new RestTemplateTossPaymentClient(restTemplate, SECRET_KEY);
    }

    @Test
    @DisplayName("paymentKey 로 결제를 조회하고 시크릿 키를 Basic 인증으로 보낸다")
    void findByPaymentKey_parsesPaymentAndSendsBasicAuth() {
        // Arrange
        String basic = "Basic " + Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
        server.expect(requestTo(PAYMENTS_URL + "pk_test_123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", basic))
                .andRespond(withSuccess("""
                        {"paymentKey":"pk_test_123","orderId":"ORD-1","status":"DONE","totalAmount":63500}
                        """, MediaType.APPLICATION_JSON));

        // Act
        Optional<TossPaymentInfo> result = client.findByPaymentKey("pk_test_123");

        // Assert
        assertThat(result).contains(new TossPaymentInfo("DONE", "ORD-1", 63_500L));
        server.verify();
    }

    @Test
    @DisplayName("totalAmount 가 없으면 amount 를 사용한다")
    void findByPaymentKey_fallsBackToAmountField() {
        // Arrange
        server.expect(requestTo(PAYMENTS_URL + "pk_x"))
                .andRespond(withSuccess("{\"orderId\":\"ORD-2\",\"status\":\"DONE\",\"amount\":1000}",
                        MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThat(client.findByPaymentKey("pk_x")).contains(new TossPaymentInfo("DONE", "ORD-2", 1_000L));
    }

    @Test
    @DisplayName("토스가 404를 돌려주면 결제가 없는 것으로 본다")
    void findByPaymentKey_returnsEmptyWhenNotFound() {
        // Arrange
        server.expect(requestTo(PAYMENTS_URL + "pk_missing")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThat(client.findByPaymentKey("pk_missing")).isEmpty();
    }

    @Test
    @DisplayName("토스가 5xx 또는 인증 실패를 돌려주면 확인할 수 없는 상태(TossApiUnavailableException)로 처리한다")
    void findByPaymentKey_throwsUnavailableOnServerOrAuthError() {
        // Arrange
        server.expect(requestTo(PAYMENTS_URL + "pk_500")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo(PAYMENTS_URL + "pk_401")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // Act & Assert
        assertThatThrownBy(() -> client.findByPaymentKey("pk_500")).isInstanceOf(TossApiUnavailableException.class);
        assertThatThrownBy(() -> client.findByPaymentKey("pk_401")).isInstanceOf(TossApiUnavailableException.class);
    }

    @Test
    @DisplayName("시크릿 키가 없으면 토스를 호출하지 않고 확인할 수 없는 상태로 처리한다")
    void findByPaymentKey_throwsUnavailableWhenSecretKeyMissing() {
        // Arrange
        RestTemplateTossPaymentClient noKey = new RestTemplateTossPaymentClient(new RestTemplate(), " ");

        // Act & Assert
        assertThatThrownBy(() -> noKey.findByPaymentKey("pk_any")).isInstanceOf(TossApiUnavailableException.class);
    }

    @Test
    @DisplayName("paymentKey 는 경로 값으로 인코딩되어 경로 조작(../)이 불가능하다")
    void findByPaymentKey_encodesPaymentKeyAsPathValue() {
        // Arrange
        server.expect(requestTo(PAYMENTS_URL + "a%2F..%2Fb"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThat(client.findByPaymentKey("a/../b")).isEmpty();
        server.verify();
    }
}
