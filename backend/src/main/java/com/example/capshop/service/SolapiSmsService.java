package com.example.capshop.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.capshop.util.solapi.SolapiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SolapiSmsService {

    private final ObjectMapper objectMapper;
    private final SolapiClient solapiClient;

    private final String apiKey;
    private final String apiSecret;
    private final String fromNumber;
    private final boolean debugLogResponse;

    public SolapiSmsService(
            ObjectMapper objectMapper,
            @Value("${solapi.api-key:}") String apiKey,
            @Value("${solapi.api-secret:}") String apiSecret,
            @Value("${solapi.from-number:}") String fromNumber,
            @Value("${solapi.debug-log-response:false}") boolean debugLogResponse) {
        this.objectMapper = objectMapper;
        this.solapiClient = new SolapiClient();
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.fromNumber = fromNumber;
        this.debugLogResponse = debugLogResponse;
    }

    public void sendVerificationCode(String toPhoneNumber, String code) {
        if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank() || fromNumber == null
                || fromNumber.isBlank()) {
            throw new IllegalStateException("문자 발송 설정이 누락되었습니다. (solapi.api-key / solapi.api-secret / solapi.from-number)");
        }

        String to = normalizePhone(toPhoneNumber);
        String from = normalizePhone(fromNumber);
        String text = "[Hey!Mr.Trucker] 인증번호는 " + code + " 입니다.";

        try {
            // Solapi send-many/detail는 일반적으로 messages 배열을 받습니다.
            String messageJson = objectMapper.writeValueAsString(Map.of(
                    "messages", List.of(Map.of(
                            "to", to,
                            "from", from,
                            "text", text))));

            String responseBody = solapiClient.sendManyDetail(apiKey, apiSecret, messageJson);

            if (debugLogResponse) {
                log.info("Solapi response (masked)={}", maskSensitive(responseBody));
            }

            // 2xx라도 메시지 단위 실패가 있을 수 있어, 응답을 JSON으로 파싱해 힌트를 남김
            String failureHint = extractFailureHint(responseBody);
            if (failureHint != null) {
                log.warn("Solapi SMS accepted but indicates failure: to={}, hint={}", maskPhone(to), failureHint);
            } else {
                log.info("Solapi SMS accepted: to={}, bytes={}", maskPhone(to), responseBody != null ? responseBody.length() : 0);
            }
        } catch (Exception e) {
            log.warn("Solapi SMS send failed: to={}, reason={}", maskPhone(toPhoneNumber), e.getMessage());
            throw new IllegalStateException("문자 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    public void sendOrderCompleted(String toPhoneNumber, String orderNumber, String trackingUrl) {
        sendOrderCompleted(toPhoneNumber, orderNumber, trackingUrl, null, null);
    }

    public void sendOrderCompleted(
            String toPhoneNumber,
            String orderNumber,
            String trackingUrl,
            String receiverName,
            String address) {
        if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank() || fromNumber == null
                || fromNumber.isBlank()) {
            throw new IllegalStateException("문자 발송 설정이 누락되었습니다. (solapi.api-key / solapi.api-secret / solapi.from-number)");
        }

        String to = normalizePhone(toPhoneNumber);
        String from = normalizePhone(fromNumber);
        String orderNo = (orderNumber == null || orderNumber.isBlank()) ? "-" : orderNumber;
        String url = (trackingUrl == null || trackingUrl.isBlank()) ? "" : trackingUrl;

        String name = receiverName == null ? "" : receiverName.trim();
        String addr = address == null ? "" : address.trim();

        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("주문이 완료되었습니다.\n");
        textBuilder.append("주문번호: ").append(orderNo).append("\n");
        if (!name.isBlank()) {
            textBuilder.append("수령인: ").append(name).append("\n");
        }
        if (!addr.isBlank()) {
            textBuilder.append("주소: ").append(addr).append("\n");
        }
        if (!url.isBlank()) {
            textBuilder.append("진행사항 확인하기: ").append(url);
        }
        String text = textBuilder.toString().trim();

        try {
            String messageJson = objectMapper.writeValueAsString(Map.of(
                    "messages", List.of(Map.of(
                            "to", to,
                            "from", from,
                            "text", text))));

            String responseBody = solapiClient.sendManyDetail(apiKey, apiSecret, messageJson);

            if (debugLogResponse) {
                log.info("Solapi response (masked)={}", maskSensitive(responseBody));
            }

            String failureHint = extractFailureHint(responseBody);
            if (failureHint != null) {
                log.warn("Solapi order SMS accepted but indicates failure: to={}, hint={}", maskPhone(to), failureHint);
            } else {
                log.info("Solapi order SMS accepted: to={}, bytes={}", maskPhone(to), responseBody != null ? responseBody.length() : 0);
            }
        } catch (Exception e) {
            log.warn("Solapi order SMS send failed: to={}, reason={}", maskPhone(toPhoneNumber), e.getMessage());
            // 주문 성공 자체는 유지되어야 하므로 예외는 외부로 던지지 않음
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }

    private String maskPhone(String phone) {
        String p = normalizePhone(phone);
        if (p.length() <= 4) return "****";
        return "****" + p.substring(p.length() - 4);
    }

    private String extractFailureHint(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Solapi 응답 스키마가 프로젝트마다 다를 수 있어, 흔히 쓰는 실패 필드를 넓게 탐색
            if (root.hasNonNull("errorCode") || root.hasNonNull("errorMessage") || root.hasNonNull("error")) {
                return root.path("errorCode").asText(null) + ":" + root.path("errorMessage").asText(root.path("error").asText(null));
            }

            // 실패 카운트/리스트가 있는 경우
            for (String key : new String[] { "failedCount", "failedMessageCount", "failed" }) {
                JsonNode node = root.get(key);
                if (node != null && node.isInt() && node.asInt() > 0) {
                    return key + "=" + node.asInt();
                }
            }

            for (String key : new String[] { "failedMessageList", "failedMessages", "errors" }) {
                JsonNode node = root.get(key);
                if (node != null && node.isArray() && node.size() > 0) {
                    return key + " size=" + node.size();
                }
            }
        } catch (Exception ignore) {
            // JSON이 아니거나 파싱 실패면 힌트 없이 넘어감
        }
        return null;
    }

    private String maskSensitive(String raw) {
        if (raw == null) return null;
        // 숫자 7자리 이상 연속 구간을 마스킹(마지막 4자리만 남김)
        // 예: 01012345678 -> ****5678
        return raw.replaceAll("\\d{3,}(\\d{4})", "****$1");
    }
}
