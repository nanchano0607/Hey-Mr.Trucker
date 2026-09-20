package com.example.capshop.service.order;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.capshop.dto.order.TossPaymentInfo;
import com.fasterxml.jackson.databind.JsonNode;

/** RestTemplate 로 토스 결제 조회 API(GET /v1/payments/{paymentKey})를 호출한다. 연결·읽기 타임아웃을 둔다. */
@Component
public class RestTemplateTossPaymentClient implements TossPaymentClient {

    private static final String PAYMENTS_URL = "https://api.tosspayments.com/v1/payments/{paymentKey}";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestTemplate restTemplate;
    private final String secretKey;

    @Autowired
    public RestTemplateTossPaymentClient(
            RestTemplateBuilder builder,
            @Value("${toss.payments.secret-key:}") String secretKey) {
        this(builder.connectTimeout(CONNECT_TIMEOUT).readTimeout(READ_TIMEOUT).build(), secretKey);
    }

    public RestTemplateTossPaymentClient(RestTemplate restTemplate, String secretKey) {
        this.restTemplate = restTemplate;
        this.secretKey = secretKey;
    }

    @Override
    public Optional<TossPaymentInfo> findByPaymentKey(String paymentKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new TossApiUnavailableException("토스 시크릿 키가 설정되지 않았습니다. toss.payments.secret-key를 설정하세요.", null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(secretKey, "");

        try {
            // paymentKey 는 URI 변수로 넘겨 경로 값으로 인코딩된다. (경로 조작 방지)
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    PAYMENTS_URL, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class, paymentKey);
            JsonNode body = response.getBody();
            if (body == null) {
                throw new TossApiUnavailableException("토스 결제 조회 응답이 비어 있습니다.", null);
            }
            return Optional.of(toInfo(body));
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        } catch (RestClientException failure) {
            throw new TossApiUnavailableException("토스 결제 조회에 실패했습니다.", failure);
        }
    }

    private TossPaymentInfo toInfo(JsonNode body) {
        long totalAmount = body.hasNonNull("totalAmount")
                ? body.get("totalAmount").asLong()
                : body.path("amount").asLong(-1L);
        return new TossPaymentInfo(body.path("status").asText(null), body.path("orderId").asText(null), totalAmount);
    }
}
