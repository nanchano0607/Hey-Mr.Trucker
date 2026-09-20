package com.example.capshop.controller.order;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.service.order.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.capshop.service.order.TossApiUnavailableException;
import com.example.capshop.service.order.DepositNotVerifiedException;
import org.springframework.http.HttpStatus;

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

            // 웹훅 본문은 신뢰하지 않는다. 주문 확정 전에 서비스가 토스에 직접 조회해 입금 완료를 확인한다.
            orderService.onDepositDoneByOrderId(orderId);

        } catch (TossApiUnavailableException e) {
            // 토스를 조회하지 못해 확인하지 못했다. 정상 입금일 수 있으므로 토스가 웹훅을 재시도하도록 5xx 로 응답한다.
            log.error("[VA_WEBHOOK] 토스 조회 실패로 입금을 확인하지 못했습니다. 재시도를 위해 503 응답: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (DepositNotVerifiedException e) {
            // 위조 웹훅이거나 아직 입금되지 않았다. 재시도할 필요가 없으므로 200 으로 응답하고 확정하지 않는다.
            log.warn("[VA_WEBHOOK] 입금을 확인하지 못해 무시합니다: {}", e.getMessage());
        } catch (Exception e) {
            // 500 반환 금지(재시도 폭주 방지) → 내부 로깅만
            log.error("Webhook error (return 200 to avoid retries storm)", e);
        }

        return ResponseEntity.ok().build();
    }
}


