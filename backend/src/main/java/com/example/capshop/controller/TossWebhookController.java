package com.example.capshop.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/toss")
@RequiredArgsConstructor
@Slf4j
public class TossWebhookController {

    private final OrderService orderService;

   @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {
        log.info("===== [TOSS WEBHOOK RECEIVED] ===== payload={}", payload);

        try {
            String orderId = payload.get("orderId") == null ? null : String.valueOf(payload.get("orderId"));
            String status = payload.get("status") == null ? null : String.valueOf(payload.get("status"));
            String txKey = payload.get("transactionKey") == null ? null : String.valueOf(payload.get("transactionKey"));

            log.info("[VA_WEBHOOK] orderId={}, status={}, txKey={}", orderId, status, txKey);

            if (orderId == null || status == null) {
                log.warn("[VA_WEBHOOK] missing required fields. orderId={}, status={}", orderId, status);
                return ResponseEntity.ok().build();
            }

            // ✅ 입금 완료만 처리
            if (!"DONE".equalsIgnoreCase(status)) {
                log.info("[VA_WEBHOOK] skip status={}", status);
                return ResponseEntity.ok().build();
            }

            // ✅ 토스 API 조회 금지: DB 기준으로 paymentKey 찾아 주문 확정
            orderService.onDepositDoneByOrderId(orderId);

        } catch (Exception e) {
            // 500 반환 금지(재시도 폭주 방지) → 내부 로깅만
            log.error("Webhook error (return 200 to avoid retries storm)", e);
        }

        return ResponseEntity.ok().build();
    }
}


