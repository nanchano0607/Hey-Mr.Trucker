package com.example.capshop.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.Status;
import com.example.capshop.domain.User;
import com.example.capshop.domain.order.Order;
import com.example.capshop.dto.OrderResponse;
import com.example.capshop.dto.RefundAccountRequest;
import com.example.capshop.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    // 결제 승인 + 주문 생성 (토스 결제 성공 후 호출)
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmPayment(
        @AuthenticationPrincipal User user,
        @RequestBody Map<String, Object> request) {

    log.info("===== [CONFIRM START] =====");
    log.info("auth user = {}", user != null ? user.getId() : "NULL");
    log.info("request body = {}", request);

    try {
        Object paymentKeyObj = request.get("paymentKey");
        Object orderIdObj = request.get("orderId");
        Object amountObj = request.get("amount");
        Object discountInfoObj = request.get("discountInfo");

        log.info("paymentKey = {} ({})", paymentKeyObj, paymentKeyObj != null ? paymentKeyObj.getClass() : null);
        log.info("orderId = {} ({})", orderIdObj, orderIdObj != null ? orderIdObj.getClass() : null);
        log.info("amount = {} ({})", amountObj, amountObj != null ? amountObj.getClass() : null);
        log.info("discountInfo = {}", discountInfoObj);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }

        if (paymentKeyObj == null || orderIdObj == null || amountObj == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "필수 파라미터 누락"));
        }

        Long amount = (amountObj instanceof Number)
                ? ((Number) amountObj).longValue()
                : Long.parseLong(String.valueOf(amountObj));

        @SuppressWarnings("unchecked")
        Map<String, Object> discountInfo =
                discountInfoObj instanceof Map ? (Map<String, Object>) discountInfoObj : null;

        log.info("parsed amount = {}", amount);

        Order order = orderService.confirmPaymentAndCreateOrderWithDiscount(
                user,
                String.valueOf(paymentKeyObj),
                String.valueOf(orderIdObj),
                amount,
                discountInfo
        );

        log.info("===== [CONFIRM SUCCESS] orderId={}, status={} =====",
                order.getId(), order.getStatus());

        return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "orderNumber", order.getOrderId(),
                "status", order.getStatus(),
                "totalPrice", order.getTotal_price()
        ));

    } catch (Exception e) {
        log.error("===== [CONFIRM ERROR] =====", e);
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }
}

    // 주문 생성 (장바구니 → 주문)
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@AuthenticationPrincipal User user) {
        try {
            Order order = orderService.placeOrder(user);
            return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "totalPrice", order.getTotal_price(),
                "status", order.getStatus(),
                "message", "주문이 생성되었습니다."
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 내 주문 목록 조회
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal User user) {
        List<Order> orders = orderService.getOrdersByUser(user);
        List<OrderResponse> response = orders.stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable("orderId") Long orderId) {
        try {
            Order order = orderService.getOrderDetail(orderId);
            return ResponseEntity.ok(new OrderResponse(order));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 주문 취소
@PostMapping("/{orderId}/cancel")
public ResponseEntity<Map<String, String>> cancelOrder(
        @PathVariable("orderId") Long orderId,
        @RequestBody(required = false) RefundAccountRequest refundReq
) {
    try {
        log.info("===== [ORDER CANCEL] orderId={}, refundReq={}", orderId, refundReq);
        orderService.cancelOrder(orderId, refundReq);
        return ResponseEntity.ok(Map.of("message", "주문이 취소되었습니다."));
    } catch (IllegalStateException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (RuntimeException e) {
        return ResponseEntity.notFound().build();
    }
}

    // 반품 요청 취소
    @PostMapping("/{orderId}/cancel-return")
    public ResponseEntity<Map<String, String>> cancelReturn(@PathVariable("orderId") Long orderId) {
        try {
            orderService.cancelReturn(orderId);
            return ResponseEntity.ok(Map.of("message", "반품 요청이 취소되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


    // 반품 요청
    @PostMapping("/{orderId}/return")
    public ResponseEntity<Map<String, String>> requestReturn(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> request) {
        log.info("[API] 반품 요청 API 호출 - orderId: {}, userId: {}", orderId, user != null ? user.getId() : "NULL");
        log.info("[API] 반품 요청 파라미터: {}", request);
        try {
            if (user == null) {
                log.error("[API] 반품 요청 실패 - 인증 필요");
                return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
            }
            
            String returnReason = (String) request.get("returnReason");
            String returnMethod = (String) request.get("returnMethod");
            Object shippingFeeObj = request.get("returnShippingFee");
            Long returnShippingFee = shippingFeeObj != null ? 
                (shippingFeeObj instanceof Integer ? ((Integer) shippingFeeObj).longValue() : (Long) shippingFeeObj) : 0L;
            
            log.info("[API] 반품 요청 내용 - 사용자: {}, 사유: {}, 방법: {}, 비용: {}원", 
                user.getEmail(), returnReason, returnMethod, returnShippingFee);
            
            orderService.requestReturn(orderId, user, returnReason, returnMethod, returnShippingFee);
            log.info("[API] 반품 요청 성공 - orderId: {}", orderId);
            return ResponseEntity.ok(Map.of("message", "반품이 요청되었습니다."));
        } catch (IllegalStateException e) {
            log.error("[API] 반품 요청 실패 (IllegalStateException) - orderId: {}, error: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("[API] 반품 요청 실패 (RuntimeException) - orderId: {}, error: {}", orderId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    // 구매확정 (사용자용)
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Map<String, String>> confirmPurchase(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
            }
            
            orderService.confirmPurchase(orderId, user.getId());
            return ResponseEntity.ok(Map.of("message", "구매가 확정되었습니다."));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
