package com.example.capshop.service;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.ProductStock;
import com.example.capshop.domain.CartItem;
import com.example.capshop.domain.PaymentStatus;
import com.example.capshop.domain.Status;
import com.example.capshop.domain.User;
import com.example.capshop.domain.order.CheckOut;
import com.example.capshop.domain.order.Order;
import com.example.capshop.domain.order.OrderItem;
import com.example.capshop.domain.order.Payment;
import com.example.capshop.dto.PointsRequest;
import com.example.capshop.dto.RefundAccountRequest;
import com.example.capshop.repository.CartItemRepository;
import com.example.capshop.repository.OrderRepository;
import com.example.capshop.repository.PaymentRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CheckOutService checkOutService;
    private final com.example.capshop.repository.ProductRepository productRepository;
    private final com.example.capshop.repository.ProductStockRepository productStockRepository;
    private final PaymentRepository paymentRepository;
    private final PointsService pointsService;
    private final UserCouponService userCouponService;
    private final SolapiSmsService solapiSmsService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.order-tracking-url:http://heymrtrucker.com/order}")
    private String orderTrackingUrl;

    @Value("${toss.payments.secret-key:}")
    private String tossPaymentsSecretKey;

    private String getTossSecretKeyOrNull() {
        if (tossPaymentsSecretKey == null || tossPaymentsSecretKey.isBlank()) {
            return null;
        }
        return tossPaymentsSecretKey;
    }
    
    public OrderService(OrderRepository orderRepository,
                       CartItemRepository cartItemRepository,
                       CheckOutService checkOutService,
                       com.example.capshop.repository.ProductRepository productRepository,
                       com.example.capshop.repository.ProductStockRepository productStockRepository,
                       PaymentRepository paymentRepository,
                       PointsService pointsService,
                       UserCouponService userCouponService,
                       SolapiSmsService solapiSmsService) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.checkOutService = checkOutService;
        this.productRepository = productRepository;
        this.productStockRepository = productStockRepository;
        this.paymentRepository = paymentRepository;
        this.pointsService = pointsService;
        this.userCouponService = userCouponService;
        this.solapiSmsService = solapiSmsService;
        
        // RestTemplate UTF-8 설정
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters()
            .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    // 재고 차감: 비관적 락(FOR UPDATE)으로 해당 재고 행을 잠근 뒤 확인/차감한다.
    private void decreaseStockLocked(Long productId, String size, int quantity, String productName) {
        if (size != null && !size.isBlank()) {
            ProductStock productStock = productStockRepository.findByProductIdAndSizeForUpdate(productId, size)
                    .orElseThrow(() -> new IllegalStateException(
                            "ProductStock 정보를 찾을 수 없습니다. productId=" + productId + ", size=" + size));
            if (productStock.getStock() == null || productStock.getStock() < quantity) {
                throw new IllegalStateException("재고 부족: " + productName + " (사이즈: " + size + ", 재고: " + productStock.getStock() + ")");
            }
            productStock.decreaseStock(quantity);
        } else {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));
            if (product.getStock() == null || product.getStock() < quantity) {
                throw new IllegalStateException("재고 부족: " + productName);
            }
            product.setStock(product.getStock() - quantity);
        }
    }

    // 재고 복구(취소/반품): 동일하게 락을 잡고 증가시켜 동시 취소/반품 간 lost update를 막는다.
    private void increaseStockLocked(Long productId, String size, int quantity) {
        if (size != null && !size.isBlank()) {
            ProductStock productStock = productStockRepository.findByProductIdAndSizeForUpdate(productId, size)
                    .orElse(null);
            if (productStock != null) {
                productStock.increaseStock(quantity);
            } else {
                logger.warn("재고 복구 실패 - ProductStock을 찾을 수 없음: productId={}, size={}", productId, size);
            }
        } else {
            Product product = productRepository.findByIdForUpdate(productId).orElse(null);
            if (product != null && product.getStock() != null) {
                product.setStock(product.getStock() + quantity);
            }
        }
    }

    // 한 주문 내 여러 상품에 락을 걸 때 항상 (productId, size) 오름차순으로 잠가 데드락을 방지한다.
    private static final java.util.Comparator<OrderItem> ORDER_ITEM_LOCK_ORDER =
            java.util.Comparator.<OrderItem, Long>comparing(oi -> oi.getProduct().getId())
                    .thenComparing(oi -> oi.getSelectedSize() == null ? "" : oi.getSelectedSize());

    // CheckOut.itemsJson(JsonNode 배열)도 동일한 (productId, size) 순서로 정렬해 락 순서를 통일한다.
    private static List<JsonNode> sortItemsForLocking(JsonNode itemsNode) {
        List<JsonNode> items = new java.util.ArrayList<>();
        itemsNode.forEach(items::add);
        items.sort(java.util.Comparator.<JsonNode, Long>comparing(n -> n.get("productId").asLong())
                .thenComparing(n -> n.has("size") ? n.get("size").asText() : ""));
        return items;
    }

    private void sendOrderCompletedSmsAfterCommit(User user, Order order) {
        if (order == null) return;

        String orderPhone = order.getPhone();
        String userPhone = user != null ? user.getPhone() : null;
        String toPhone = (orderPhone != null && !orderPhone.isBlank())
                ? orderPhone
                : (userPhone != null && !userPhone.isBlank() ? userPhone : null);

        if (toPhone == null) {
            return;
        }

        String orderNo = (order.getOrderId() != null && !order.getOrderId().isBlank())
                ? order.getOrderId()
                : String.valueOf(order.getId());

        final String finalToPhone = toPhone;
        final String receiverName = order.getReceiverName();
        final String address = order.getAddress();
        Runnable sender = () -> solapiSmsService.sendOrderCompleted(finalToPhone, orderNo, orderTrackingUrl, receiverName, address);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sender.run();
                }
            });
        } else {
            sender.run();
        }
    }

    private void cancelTossPaymentSafely(String paymentKey, Long cancelAmount, String cancelReason) {
        if (paymentKey == null || paymentKey.isBlank() || cancelAmount == null || cancelAmount <= 0) {
            logger.warn("토스 결제취소 생략 - paymentKey/amount가 유효하지 않습니다. paymentKey={}, amount={}", paymentKey, cancelAmount);
            return;
        }

        try {
            String tossSecretKey = getTossSecretKeyOrNull();
            if (tossSecretKey == null) {
                logger.error("토스 결제취소 생략 - toss.payments.secret-key가 설정되지 않았습니다.");
                return;
            }
            String url = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(tossSecretKey, "");

            String reason = (cancelReason == null || cancelReason.isBlank()) ? "주문 처리 실패로 자동 취소" : cancelReason;
            String body = String.format(
                    "{\"cancelReason\":\"%s\",\"cancelAmount\":%d}",
                    reason.replace("\"", "\\\""),
                    cancelAmount
            );

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("토스 결제취소 실패 - paymentKey: {}, status: {}, body: {}",
                        paymentKey, response.getStatusCode(), response.getBody());
                return;
            }

            logger.info("토스 결제취소 성공 - paymentKey: {}, amount: {}, response: {}",
                    paymentKey, cancelAmount, response.getBody());
        } catch (Exception cancelEx) {
            logger.error("토스 결제취소 중 오류 발생 - paymentKey: {}, error: {}",
                    paymentKey, cancelEx.getMessage(), cancelEx);
        }
    }

    private void rollbackBenefitsAfterCancelOrRefund(Order order) {
        if (order == null || order.getUser() == null || order.getUser().getId() == null) {
            return;
        }

        Long userId = order.getUser().getId();

        // 1) 쿠폰 복구
        try {
            com.example.capshop.domain.UserCoupon used = order.getUsedUserCoupon();
            if (used != null) {
                used.restoreAfterOrderCancel();
                order.setUsedUserCoupon(null);
                if (order.getCoupon_discount() != null && order.getCoupon_discount() > 0) {
                    order.setCoupon_discount(0L);
                }
            }
        } catch (Exception e) {
            logger.warn("쿠폰 복구 실패 - orderId={}, reason={}", order.getId(), e.getMessage());
        }

        // 2) 사용 포인트 복구 (points_discount)
        try {
            Long usedPoints = order.getPoints_discount();
            if (usedPoints != null && usedPoints > 0) {
                PointsRequest restore = new PointsRequest();
                restore.setUserId(userId);
                restore.setAmount(usedPoints);
                restore.setReason("주문 취소/환불로 사용 적립금 복구");
                pointsService.addPoints(restore);
            }
        } catch (Exception e) {
            logger.warn("사용 적립금 복구 실패 - orderId={}, reason={}", order.getId(), e.getMessage());
        }

        // 3) 적립 포인트 회수 (설정된 적립률 기반)
        // 현재 적립 로직은 할인 결제(confirmPaymentAndCreateOrderWithDiscount)에서만 original_price 기반으로 적립함
        try {
            Long base = order.getOriginal_price();
            if (base != null && base > 0) {
                long earned = pointsService.calculateOrderPoints(base);
                if (earned > 0) {
                    Long available = order.getUser().getAvailablePoints();
                    long toDeduct = Math.min(available != null ? available : 0L, earned);
                    if (toDeduct > 0) {
                        PointsRequest deduct = new PointsRequest();
                        deduct.setUserId(userId);
                        deduct.setAmount(toDeduct);
                        deduct.setReason("주문 취소/환불로 적립금 회수");
                        pointsService.usePoints(deduct);
                    }

                    if (toDeduct < earned) {
                        logger.warn("적립금 회수 부족 - orderId={}, need={}, deducted={}, availableNow={}",
                                order.getId(), earned, toDeduct, order.getUser().getAvailablePoints());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("적립금 회수 처리 실패 - orderId={}, reason={}", order.getId(), e.getMessage());
        }
    }

    private Order findExistingOrderForIdempotency(User user, String paymentKey, String orderId) {
        Order existing = null;

        if (paymentKey != null && !paymentKey.isBlank()) {
            existing = paymentRepository.findByPaymentKey(paymentKey)
                    .map(Payment::getOrder)
                    .orElse(null);
        }

        if (existing == null && orderId != null && !orderId.isBlank()) {
            existing = orderRepository.findByOrderId(orderId).orElse(null);
        }

        if (existing != null && user != null && existing.getUser() != null
                && existing.getUser().getId() != null
                && !existing.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("주문 정보가 일치하지 않습니다.");
        }

        return existing;
    }

    @Transactional
    public Order placeOrder(User user) {
        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("장바구니가 비어있습니다.");
        }

        Order order = new Order(user);

        // 여러 상품을 동시에 잠글 때 (productId, size) 오름차순으로 락을 걸어 데드락을 방지한다.
        List<CartItem> sortedCartItems = cartItems.stream()
                .sorted(java.util.Comparator.<CartItem, Long>comparing(ci -> ci.getProduct().getId())
                        .thenComparing(ci -> ci.getSize() == null ? "" : ci.getSize()))
                .collect(java.util.stream.Collectors.toList());

        for (CartItem cartItem : sortedCartItems) {
            Product product = cartItem.getProduct();
            String size = cartItem.getSize();

            decreaseStockLocked(product.getId(), size, cartItem.getQuantity(), product.getName());

            OrderItem orderItem = new OrderItem(product, cartItem.getQuantity(), product.getPrice(), size);
            order.addOrderItem(orderItem);
        }

        order.calculateTotalPrice();

        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);

        return savedOrder;
    }

    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUser(user);
    }
    
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));
    }
    
    // 관리자용: 전체 주문 목록
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    // 관리자용: 상태별 주문 필터
    public List<Order> getOrdersByStatus(Status status) {
        return orderRepository.findByStatus(status);
    }

    public Order getOrderDetail(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));
    }

    @Transactional
public void cancelOrder(Long orderId, RefundAccountRequest refundReq) {
    logger.info("주문 취소 시작 - orderId: {}", orderId);

    Order order = getOrderDetail(orderId);

    if (!order.isCancellable()) {
        logger.warn("취소 불가능한 주문 - orderId: {}, status: {}", orderId, order.getStatus());
        throw new IllegalStateException("취소할 수 없는 주문입니다.");
    }

    boolean isVA = order.isVirtualAccount();
    logger.info("취소 요청 - isVirtualAccount={}, refundReqNull={}", isVA, (refundReq == null));

    // ✅ 가상계좌면 환불계좌 필수
    if (isVA) {
        if (refundReq == null
                || refundReq.getRefundBank() == null || refundReq.getRefundBank().isBlank()
                || refundReq.getRefundAccount() == null || refundReq.getRefundAccount().isBlank()
                || refundReq.getRefundHolder() == null || refundReq.getRefundHolder().isBlank()) {
            throw new IllegalStateException("가상계좌 결제 건은 환불 계좌 정보가 필요합니다.");
        }
    }

    // 주문 상태 취소
    order.cancel();
    logger.info("주문 상태 CANCELLED로 변경 - orderId: {}", orderId);

    // 재고 복구 (동시 취소/반품 간 lost update 방지를 위해 락 순서 정렬 후 처리)
    List<OrderItem> sortedItemsForCancel = order.getOrderItems().stream()
            .sorted(ORDER_ITEM_LOCK_ORDER)
            .collect(java.util.stream.Collectors.toList());
    for (OrderItem item : sortedItemsForCancel) {
        increaseStockLocked(item.getProduct().getId(), item.getSelectedSize(), item.getQuantity());
    }

    // 결제 취소(토스)
    Payment payment = paymentRepository.findByOrder(order)
            .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));

    logger.info("토스 결제 취소 요청 - paymentKey: {}, isVA={}", payment.getPaymentKey(), isVA);

    // ✅ 여기서 가상계좌면 refundReceiveAccount 포함해서 토스로 cancel
    cancelPaymentToToss(payment, isVA ? refundReq : null);

    payment.cancel();
    paymentRepository.save(payment);

    // 쿠폰/포인트/적립금 롤백
    rollbackBenefitsAfterCancelOrRefund(order);

    orderRepository.save(order);
    logger.info("주문 취소 완료 - orderId: {}", orderId);
}
    
    // 반품 요청
    @Transactional
    public void requestReturn(Long orderId, User user, String returnReason, String returnMethod, Long returnShippingFee) {
        logger.info("===== 반품 요청 시작 =====");
        logger.info("주문 ID: {}", orderId);
        logger.info("사용자 ID: {}, 사용자 이메일: {}", user.getId(), user.getEmail());
        logger.info("반품 사유: {}", returnReason);
        logger.info("반품 방법: {}", returnMethod);
        
        Order order = getOrderDetail(orderId);
        logger.info("주문 조회 완료 - 주문번호: {}, 현재 상태: {}", order.getOrderId(), order.getStatus());
        
        // 본인 주문인지 확인
        if (!order.getUser().getId().equals(user.getId())) {
            logger.error("권한 없음 - 주문 소유자 ID: {}, 요청자 ID: {}", order.getUser().getId(), user.getId());
            throw new IllegalStateException("본인의 주문만 반품 요청할 수 있습니다.");
        }
        
        // 반품 사유 유효성 검증
        if (returnReason == null || returnReason.isBlank()) {
            logger.error("반품 사유 누락 - orderId: {}", orderId);
            throw new IllegalStateException("반품 사유를 입력해주세요.");
        }
        
        // 반품 방법 유효성 검증 - PICKUP만 허용
        if (returnMethod == null || returnMethod.isBlank()) {
            logger.error("반품 방법 누락 - orderId: {}", orderId);
            throw new IllegalStateException("반품 방법을 선택해주세요.");
        }
        if (!"PICKUP".equals(returnMethod)) {
            logger.error("잘못된 반품 방법: {} - orderId: {}", returnMethod, orderId);
            throw new IllegalStateException("현재 회수 요청만 가능합니다.");
        }
        
        // 회수 요청은 배송비 무료
        order.setReturnShippingFee(0L);
        order.setReturnReason(returnReason);
        order.setReturnMethod(returnMethod);
        logger.info("반품 정보 설정 완료 - 배송비: 0원 (회수 요청)");
        
        order.requestReturn();
        logger.info("주문 상태 변경: {} -> RETURN_REQUESTED", order.getStatus());
        
        orderRepository.save(order);
        logger.info("===== 반품 요청 완료 - orderId: {} =====", orderId);
    }
    
    // 반품 승인 (관리자용) - 반품 배송 시작
    @Transactional
    public void approveReturn(Long orderId, String returnTrackingNumber) {
        logger.info("===== 반품 승인 시작 =====");
        logger.info("주문 ID: {}", orderId);
        logger.info("반품 송장번호: {}", returnTrackingNumber);
        
        Order order = getOrderDetail(orderId);
        logger.info("주문 조회 완료 - 주문번호: {}, 현재 상태: {}", order.getOrderId(), order.getStatus());
        logger.info("반품 사유: {}, 반품 방법: {}", order.getReturnReason(), order.getReturnMethod());
        
        if (returnTrackingNumber == null || returnTrackingNumber.isBlank()) {
            logger.error("반품 송장번호 누락 - orderId: {}", orderId);
            throw new IllegalStateException("반품 송장번호가 필요합니다.");
        }
        
        order.setReturnTrackingNumber(returnTrackingNumber.trim());
        logger.info("반품 송장번호 설정 완료: {}", returnTrackingNumber.trim());
        
        order.approveReturn(); // RETURN_SHIPPING으로 변경
        logger.info("주문 상태 변경: RETURN_REQUESTED -> RETURN_SHIPPING");
        
        orderRepository.save(order);
        logger.info("===== 반품 승인 완료 - orderId: {} =====", orderId);
    }
    
    // 반품 완료 (관리자용) - 상품 도착 확인 후 환불
    @Transactional
    public void completeReturn(Long orderId, Long returnShippingFee) {
        logger.info("===== 반품 완료 처리 시작 =====");
        logger.info("주문 ID: {}", orderId);
        logger.info("차감할 반품 배송비: {}원", returnShippingFee != null ? returnShippingFee : 0);
        
        Order order = getOrderDetail(orderId);
        logger.info("주문 조회 완료 - 주문번호: {}, 현재 상태: {}", order.getOrderId(), order.getStatus());
        logger.info("반품 송장번호: {}", order.getReturnTrackingNumber());
        
        // 반품 배송비 설정
        if (returnShippingFee != null && returnShippingFee > 0) {
            order.setReturnShippingFee(returnShippingFee);
            logger.info("반품 배송비 설정 - orderId: {}, fee: {}", orderId, returnShippingFee);
        }
        
        order.completeReturn(); // RETURNED로 변경
        logger.info("주문 상태 RETURNED로 변경 - orderId: {}", orderId);
        
        // 재고 복구 (동시 취소/반품 간 lost update 방지를 위해 락 순서 정렬 후 처리)
        List<OrderItem> sortedItemsForReturn = order.getOrderItems().stream()
                .sorted(ORDER_ITEM_LOCK_ORDER)
                .collect(java.util.stream.Collectors.toList());
        for (OrderItem item : sortedItemsForReturn) {
            increaseStockLocked(item.getProduct().getId(), item.getSelectedSize(), item.getQuantity());
            logger.info("반품 재고 복구 - productId: {}, size: {}, 수량: {}",
                item.getProduct().getId(), item.getSelectedSize(), item.getQuantity());
        }
        
        // 환불 처리
        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));
        
        // 환불 금액 계산 (결제 금액 - 반품 택배비)
        Long refundAmount = payment.getAmount();
        Long shippingFee = order.getReturnShippingFee() != null ? order.getReturnShippingFee() : 0L;
        Long actualRefundAmount = refundAmount - shippingFee;
        
        if (actualRefundAmount < 0) {
            throw new IllegalStateException("환불 금액이 유효하지 않습니다.");
        }
        
        String refundReason = "반품 완료";
        if (shippingFee > 0) {
            refundReason += " (택배비 " + shippingFee + "원 차감)";
        }
        
        logger.info("토스 환불 요청 - paymentKey: {}, 원결제금액: {}, 택배비: {}, 실환불금액: {}", 
            payment.getPaymentKey(), payment.getAmount(), shippingFee, actualRefundAmount);
        
        if (actualRefundAmount > 0) {
            refundPaymentToToss(payment, actualRefundAmount, refundReason);
            if (shippingFee > 0) {
                payment.partialRefund(); // 부분 환불
            } else {
                payment.refund(); // 전액 환불
            }
        } else {
            logger.warn("환불 금액이 0원입니다. 환불 처리를 건너뜁니다.");
        }
        
        paymentRepository.save(payment);
        logger.info("Payment 상태 변경 완료 - paymentId: {}, 상태: {}", payment.getId(), payment.getStatus());

        // 쿠폰/포인트/적립금 롤백 (환불 성공 후)
        logger.info("쿠폰/포인트/적립금 롤백 시작 - orderId: {}", orderId);
        rollbackBenefitsAfterCancelOrRefund(order);
        logger.info("쿠폰/포인트/적립금 롤백 완료");
        
        orderRepository.save(order);
        logger.info("===== 반품 완료 처리 완료 - orderId: {} =====", orderId);
    }

    // 반품 취소 (관리자용)
    @Transactional
    public void cancelReturn(Long orderId) {
        logger.info("===== 반품 취소 처리 시작 =====");
        logger.info("주문 ID: {}", orderId);
        
        Order order = getOrderDetail(orderId);
        logger.info("주문 조회 완료 - 주문번호: {}, 현재 상태: {}", order.getOrderId(), order.getStatus());
        logger.info("취소할 반품 정보 - 사유: {}, 방법: {}, 송장번호: {}", 
            order.getReturnReason(), order.getReturnMethod(), order.getReturnTrackingNumber());

        order.cancelReturn();
        logger.info("주문 상태 변경: RETURN_REQUESTED -> DELIVERED");
        
        orderRepository.save(order);
        logger.info("===== 반품 취소 완료 - orderId: {} =====", orderId);
    }
    
    // 구매확정 (사용자용)
    @Transactional
    public void confirmPurchase(Long orderId, Long userId) {
        logger.info("구매확정 처리 시작 - orderId: {}, userId: {}", orderId, userId);
        Order order = getOrderDetail(orderId);
        
        // 본인 주문인지 확인
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 주문만 구매확정할 수 있습니다.");
        }
        
        order.confirmPurchase();
        orderRepository.save(order);
        logger.info("구매확정 완료 - orderId: {}", orderId);
    }
    
    // 배송 준비중 (관리자용)
    @Transactional
    public void prepareForShipment(Long orderId) {
        logger.info("배송준비중 상태로 변경 - orderId: {}", orderId);
        Order order = getOrderDetail(orderId);
        
        if (order.getStatus() != Status.ORDERED) {
            throw new IllegalStateException("상품 준비중 상태에서만 배송준비중으로 변경 가능합니다.");
        }
        
        order.setStatus(Status.PREPARING_SHIPMENT);
        orderRepository.save(order);
        logger.info("주문 상태 PREPARING_SHIPMENT로 변경 - orderId: {}", orderId);
    }
    
    // 배송 시작 (관리자용)
    @Transactional
    public void shipOrder(Long orderId) {
        logger.info("배송 시작 - orderId: {}", orderId);
        Order order = getOrderDetail(orderId);
        
        order.ship();
        orderRepository.save(order);
        logger.info("주문 상태 SHIPPED로 변경 - orderId: {}", orderId);
    }
    
    // 배송 완료 (관리자용)
    @Transactional
    public void deliverOrder(Long orderId) {
        logger.info("배송 완료 처리 - orderId: {}", orderId);
        Order order = getOrderDetail(orderId);
        
        order.markAsDelivered();
        orderRepository.save(order);
        logger.info("주문 상태 DELIVERED로 변경 - orderId: {}, deliveredAt: {}", 
            orderId, order.getDeliveredAt());
    }

    // 송장번호 설정 (관리자용)
    @Transactional
    public void updateTrackingNumber(Long orderId, String trackingNumber) {
        logger.info("송장번호 설정 - orderId: {}, trackingNumber: {}", orderId, trackingNumber);
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalStateException("유효한 송장번호가 필요합니다.");
        }
        Order order = getOrderDetail(orderId);
        // 취소/반품 완료 후에는 수정 불가
        switch (order.getStatus()) {
            case CANCELLED:
            case RETURNED:
                throw new IllegalStateException("해당 주문 상태에서는 송장번호를 설정할 수 없습니다.");
            default:
                break;
        }
        order.setTrackingNumber(trackingNumber.trim());
        orderRepository.save(order);
        logger.info("송장번호 설정 완료 - orderId: {}", orderId);
    }
    
    // 토스 결제 취소 API 호출
   private void cancelPaymentToToss(Payment payment, RefundAccountRequest refundReq) {
    try {
        JsonNode paymentJson = getTossPaymentByKey(payment.getPaymentKey());
        JsonNode va = paymentJson.path("virtualAccount");

        logger.info("[TOSS VA] raw virtualAccount={}", va.toString());
        logger.info("[TOSS VA] bankCode={}, bank={}, accountNumber={}",
                va.path("bankCode").asText(""),
                va.path("bank").asText(""),
                va.path("accountNumber").asText("")
        );




        String tossSecretKey = getTossSecretKeyOrNull();
        if (tossSecretKey == null) {
            throw new IllegalStateException("토스 시크릿 키가 설정되지 않았습니다. toss.payments.secret-key를 설정하세요.");
        }

        String url = "https://api.tosspayments.com/v1/payments/" + payment.getPaymentKey() + "/cancel";
        logger.info("토스 API 호출 - URL: {}, paymentKey: {}", url, payment.getPaymentKey());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(tossSecretKey, "");

        // ✅ JSON은 문자열 포맷보다 ObjectMapper로 만드는 게 안전
        com.fasterxml.jackson.databind.node.ObjectNode bodyNode = objectMapper.createObjectNode();
        bodyNode.put("cancelReason", "고객 주문 취소");

        // ✅ 가상계좌 환불계좌 포함
        if (refundReq != null) {
            com.fasterxml.jackson.databind.node.ObjectNode acc = objectMapper.createObjectNode();
            acc.put("bank", refundReq.getRefundBank().trim());
            acc.put("accountNumber", refundReq.getRefundAccount().trim());
            acc.put("holderName", refundReq.getRefundHolder().trim());
            bodyNode.set("refundReceiveAccount", acc);
        }

        String body = objectMapper.writeValueAsString(bodyNode);

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            logger.error("토스 결제 취소 실패 - status: {}, body: {}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("토스 결제 취소 실패: " + response.getBody());
        }

        logger.info("토스 결제 취소 성공 - paymentKey: {}, response: {}", payment.getPaymentKey(), response.getBody());

    } catch (Exception e) {
        logger.error("결제 취소 처리 중 오류 발생 - paymentKey: {}, error: {}", payment.getPaymentKey(), e.getMessage(), e);
        throw new RuntimeException("결제 취소 처리 중 오류 발생: " + e.getMessage(), e);
    }
}
    
    // 토스 환불 API 호출
    private void refundPaymentToToss(Payment payment, Long refundAmount, String refundReason) {
        try {
            String tossSecretKey = getTossSecretKeyOrNull();
            if (tossSecretKey == null) {
                throw new IllegalStateException("토스 시크릿 키가 설정되지 않았습니다. toss.payments.secret-key를 설정하세요.");
            }
            String url = "https://api.tosspayments.com/v1/payments/" + payment.getPaymentKey() + "/cancel";
            
            logger.info("토스 환불 API 호출 - URL: {}, paymentKey: {}, amount: {}", 
                url, payment.getPaymentKey(), refundAmount);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(tossSecretKey, "");
            
            String body = String.format(
                "{\"cancelReason\":\"%s\",\"cancelAmount\":%d}",
                refundReason, refundAmount
            );
            
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("토스 환불 실패 - status: {}, body: {}", 
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("토스 환불 실패: " + response.getBody());
            }
            
            logger.info("토스 환불 성공 - paymentKey: {}, amount: {}, response: {}", 
                payment.getPaymentKey(), refundAmount, response.getBody());
        } catch (Exception e) {
            logger.error("환불 처리 중 오류 발생 - paymentKey: {}, error: {}", 
                payment.getPaymentKey(), e.getMessage(), e);
            throw new RuntimeException("환불 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public Order confirmPaymentAndCreateOrder(User user, String paymentKey, String orderId, Long amount) {
    // 멱등 처리: 동일 요청이 재시도되면 기존 주문을 반환
    Order existingOrder = findExistingOrderForIdempotency(user, paymentKey, orderId);
    if (existingOrder != null) {
        return existingOrder;
    }

    boolean paymentConfirmed = false;
    try {
        // 1. 토스에 결제 최종 승인 요청
        String tossSecretKey = getTossSecretKeyOrNull();
        if (tossSecretKey == null) {
            throw new IllegalStateException("토스 시크릿 키가 설정되지 않았습니다. toss.payments.secret-key를 설정하세요.");
        }
        String url = "https://api.tosspayments.com/v1/payments/confirm";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(tossSecretKey, "");
        
        String body = String.format(
            "{\"paymentKey\":\"%s\",\"orderId\":\"%s\",\"amount\":%d}",
            paymentKey, orderId, amount
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("토스 결제 승인 실패: " + response.getBody());
        }
        
        // 토스 응답 파싱
        JsonNode tossResponse = objectMapper.readTree(response.getBody());
        String tossStatus = tossResponse.get("status").asText(); // 토스 결제 상태
        String paymentMethod = tossResponse.has("method") ? tossResponse.get("method").asText() : "CARD";
        
        // 토스 결제 상태 검증
        if (!"DONE".equals(tossStatus)) {
            throw new RuntimeException("토스 결제가 완료되지 않았습니다. 상태: " + tossStatus);
        }
        paymentConfirmed = true;
        
        // 2. CheckOut에서 orderId로 조회
        CheckOut checkOut = checkOutService.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("체크아웃 정보를 찾을 수 없습니다: " + orderId));
        
        // 3. CheckOut의 itemsJson 파싱하여 Order + OrderItem 생성
        JsonNode itemsNode = objectMapper.readTree(checkOut.getItemsJson());
        Order order = new Order(user);
        order.setStatus(Status.ORDERED);
        
        // CheckOut의 주문번호 및 배송 정보를 Order로 복사
        order.setOrderId(checkOut.getOrderId());
        order.setReceiverName(checkOut.getName());
        order.setAddress(checkOut.getAddress());
        order.setPhone(checkOut.getPhone());
        
        for (JsonNode item : sortItemsForLocking(itemsNode)) {
            Long productId = item.get("productId").asLong();
            int quantity = item.get("quantity").asInt();
            String size = item.has("size") ? item.get("size").asText() : null;

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));

            decreaseStockLocked(productId, size, quantity, product.getName());

            // OrderItem 생성 (가격 스냅샷 + 사이즈)
            OrderItem orderItem = new OrderItem(product, quantity, product.getPrice(), size);
            order.addOrderItem(orderItem);
        }

        order.calculateTotalPrice();

        // 4. 금액 검증 (토스 승인 금액 == 계산된 주문 금액)
        if (!order.getTotal_price().equals(amount)) {
            throw new RuntimeException("결제 금액 불일치");
        }
        
        // 5. Order 저장
        Order savedOrder = orderRepository.save(order);
        
        // 6. Payment 생성 및 저장
        Payment payment = new Payment(savedOrder, paymentKey, paymentMethod, amount);
        payment.approve(); // 결제 승인 완료 상태로 변경
        paymentRepository.save(payment);
        
        // 7. CheckOut 삭제
        checkOutService.deleteById(checkOut.getId());

        // 주문 완료 문자 발송(커밋 이후) - 테스트 종료로 임시 비활성화
        sendOrderCompletedSmsAfterCommit(user, savedOrder);
        
        return savedOrder;
        
    } catch (Exception e) {
        if (paymentConfirmed) {
            cancelTossPaymentSafely(paymentKey, amount, "주문 처리 실패로 자동 취소");
        }
        throw new RuntimeException("결제 처리 중 오류 발생: " + e.getMessage(), e);
    }
}

    // 할인 정보를 포함한 결제 승인 + 주문 생성
    @Transactional
public Order confirmPaymentAndCreateOrderWithDiscount(
        User user,
        String paymentKey,
        String orderId,
        Long amount,
        Map<String, Object> discountInfo
) {
    // 0) 멱등 처리: 동일 요청이 재시도되면 기존 주문을 반환
    Order existingOrder = findExistingOrderForIdempotency(user, paymentKey, orderId);
    if (existingOrder != null) {
        return existingOrder;
    }

    boolean paymentConfirmCallSucceeded = false;

    try {
        // 1) 토스 결제 최종 승인 요청 (confirm)
        String tossSecretKey = getTossSecretKeyOrNull();
        if (tossSecretKey == null) {
            throw new IllegalStateException("토스 시크릿 키가 설정되지 않았습니다. toss.payments.secret-key를 설정하세요.");
        }

        String url = "https://api.tosspayments.com/v1/payments/confirm";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(tossSecretKey, "");

        String body = String.format(
                "{\"paymentKey\":\"%s\",\"orderId\":\"%s\",\"amount\":%d}",
                paymentKey, orderId, amount
        );

        HttpEntity<String> req = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, req, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("토스 결제 승인 실패: " + resp.getBody());
        }
        paymentConfirmCallSucceeded = true;

        JsonNode tossResponse = objectMapper.readTree(resp.getBody());
        String tossStatus = tossResponse.get("status").asText();
        String paymentMethod = tossResponse.has("method") ? tossResponse.get("method").asText() : "CARD";

        boolean isDone = "DONE".equals(tossStatus);
        boolean isWaitingDeposit = "WAITING_FOR_DEPOSIT".equals(tossStatus);

        // ✅ 가상계좌는 WAITING_FOR_DEPOSIT이 정상
        if (!isDone && !isWaitingDeposit) {
            throw new RuntimeException("토스 결제가 완료되지 않았습니다. 상태: " + tossStatus);
        }

        // 2) CheckOut 조회
        CheckOut checkOut = checkOutService.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("체크아웃 정보를 찾을 수 없습니다: " + orderId));

        // 3) items 파싱 -> Order/OrderItem 구성
        JsonNode itemsNode = objectMapper.readTree(checkOut.getItemsJson());

        Order order = new Order(user);

        // 주문 기본 정보 복사
        order.setOrderId(checkOut.getOrderId());
        order.setReceiverName(checkOut.getName());
        order.setAddress(checkOut.getAddress());
        order.setPhone(checkOut.getPhone());

        // ✅ 상태 설정: 입금대기 vs 결제완료
        if (isWaitingDeposit) {
            order.setStatus(Status.PAYMENT_PENDING); // << enum에 추가 필요
            order.setVirtualAccount(true);
        } else {
            order.setStatus(Status.ORDERED);
        }

        Long calculatedOriginalAmount = 0L;

        for (JsonNode item : sortItemsForLocking(itemsNode)) {
            Long productId = item.get("productId").asLong();
            int quantity = item.get("quantity").asInt();
            String size = item.has("size") ? item.get("size").asText() : null;

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));

            // 재고 확인 및 차감은 DONE일 때만
            if (isDone) {
                decreaseStockLocked(productId, size, quantity, product.getName());
            }

            // OrderItem 생성 (가격 스냅샷 + 사이즈)
            OrderItem orderItem = new OrderItem(product, quantity, product.getPrice(), size);
            order.addOrderItem(orderItem);

            calculatedOriginalAmount += product.getPrice() * quantity;
        }

        // total 계산(당신 기존 로직 유지)
        order.calculateTotalPrice();

        // 4) 할인 정보 파싱 (검증 포함)
        Long originalAmount = 0L;
        Long finalAmount = amount; // 결제된 최종 금액(배송비 포함일 수 있음)
        Long couponDiscount = 0L;
        Long pointsUsed = 0L;

        if (discountInfo != null) {
            if (discountInfo.get("originalAmount") != null) {
                originalAmount = ((Number) discountInfo.get("originalAmount")).longValue();
            }
            if (discountInfo.get("couponDiscount") != null) {
                couponDiscount = ((Number) discountInfo.get("couponDiscount")).longValue();
            }
            if (discountInfo.get("pointsUsed") != null) {
                pointsUsed = ((Number) discountInfo.get("pointsUsed")).longValue();
            }
            if (discountInfo.get("finalAmount") != null) {
                Long discountFinalAmount = ((Number) discountInfo.get("finalAmount")).longValue();
                if (!discountFinalAmount.equals(amount)) {
                    throw new RuntimeException("할인 정보와 결제 금액 불일치: 할인정보=" + discountFinalAmount + ", 결제금액=" + amount);
                }
            }
        }

        // 5) 원가 검증(선택)
        if (originalAmount > 0 && !calculatedOriginalAmount.equals(originalAmount)) {
            throw new RuntimeException("원가 계산 불일치: 계산된금액=" + calculatedOriginalAmount + ", 전달받은금액=" + originalAmount);
        }

        // 6) Order에 할인 정보 기록(입금대기라도 기록은 가능)
        Long baseAmount = originalAmount > 0 ? originalAmount : calculatedOriginalAmount;
        Long totalDiscount = couponDiscount + pointsUsed;

        order.setOriginal_price(baseAmount);
        order.setCoupon_discount(couponDiscount);
        order.setPoints_discount(pointsUsed);
        order.setTotal_discount(totalDiscount);
        order.setFinal_price(finalAmount);
        order.setTotal_price(finalAmount);

        // 7) Order 저장
        Order savedOrder = orderRepository.save(order);

        // 8) Payment 저장
        Payment payment = new Payment(savedOrder, paymentKey, paymentMethod, finalAmount);

        if (isWaitingDeposit) {
            payment.setStatus(PaymentStatus.READY);       // 입금대기
            // payment.approve();  // ❌ 하면 안 됨
        } else {
            payment.approve();                            // APPROVED로 바뀌는 로직이면 유지
            // 또는 payment.setStatus(PaymentStatus.APPROVED);
        }
        paymentRepository.save(payment);

        // 9) DONE일 때만 “확정 처리” 수행
        if (isDone) {
            // 쿠폰 사용 처리
            Long userCouponId = null;
            if (discountInfo != null) {
                if (discountInfo.get("userCouponId") != null) {
                    userCouponId = ((Number) discountInfo.get("userCouponId")).longValue();
                } else if (discountInfo.get("couponId") != null) {
                    userCouponId = ((Number) discountInfo.get("couponId")).longValue();
                }
            }

            if (userCouponId != null) {
                Long usedDiscount = userCouponService.markCouponUsedOnSuccess(user.getId(), userCouponId, savedOrder);
                // 서버 계산 결과를 couponDiscount에 덮어씌우고 싶으면 여기서 업데이트 가능
            }

            // 포인트 차감
            if (pointsUsed > 0) {
                PointsRequest pointsRequest = new PointsRequest();
                pointsRequest.setUserId(user.getId());
                pointsRequest.setAmount(pointsUsed);
                pointsRequest.setReason("주문 결제");
                pointsService.usePoints(pointsRequest);
            }

            // 적립금 지급(할인 전 원래 가격 기준)
            pointsService.addOrderPoints(user.getId(), baseAmount);

            // CheckOut 삭제 + 장바구니 비우기
            checkOutService.deleteById(checkOut.getId());
            cartItemRepository.deleteByUser(user);

            // 문자 발송(커밋 이후)
            sendOrderCompletedSmsAfterCommit(user, savedOrder);
        }

        // ✅ WAITING_FOR_DEPOSIT이면 여기서 끝(주문/결제 레코드만 생성됨)
        return savedOrder;

    } catch (Exception e) {
        // NOTE: WAITING_FOR_DEPOSIT일 때 cancel을 때리면 안 됨(입금대기 발급 취소 정책 고려 필요)
        // isDone 상태에서 이후 로직 실패 시에만 cancel 처리하는 게 일반적.
        // 기존 paymentConfirmed 변수 대신, 여기서는 confirm API가 성공 호출되었는지 정도만 기록함.
        throw new RuntimeException("결제 처리 중 오류 발생: " + e.getMessage(), e);
    }
}



@Transactional
    public Order onDepositDoneByOrderId(String orderId) {
        // 1) 주문 조회
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("주문이 존재하지 않습니다: " + orderId));

        // 2) 멱등 처리: 이미 PAYMENT_PENDING 아니면 스킵
        if (order.getStatus() != Status.PAYMENT_PENDING) {
            log.info("[DEPOSIT_DONE_BY_ORDER] already processed. orderId={}, status={}", orderId, order.getStatus());
            return order;
        }

        // 3) Payment 조회 (confirm 시점에 저장해둔 paymentKey/amount가 있어야 함)
        Payment payment = paymentRepository.findTopByOrderOrderByIdDesc(order)
                .orElseThrow(() -> new IllegalStateException("Payment를 찾을 수 없습니다. orderId=" + orderId));

        String paymentKey = payment.getPaymentKey();
        long finalAmount = payment.getAmount(); // confirm 시점에 검증된 금액을 저장해두는 전제

        // 4) 가상계좌 플래그(웹훅 경로는 VA로만 들어온다고 가정)
        order.setVirtualAccount(true);

        // 5) 주문 확정 로직
        finalizePendingOrder(order, finalAmount, paymentKey);

        // 6) 저장 및 반환
        return orderRepository.save(order);
    }

    /**
     * ✅ PAYMENT_PENDING → ORDERED 확정 처리 (멱등 + 검증 + 재고/쿠폰/포인트/정리)
     */
    private void finalizePendingOrder(Order order, long finalAmount, String paymentKey) {
        // 0) 상태 확인
        if (order.getStatus() != Status.PAYMENT_PENDING) {
            log.info("[FINALIZE] skip. orderId={}, status={}", order.getOrderId(), order.getStatus());
            return;
        }

        // 1) 금액 검증 (주문에 저장해둔 최종금액 vs 확정금액)
        Long expectedFinal = order.getFinal_price();
        if (expectedFinal != null && expectedFinal > 0 && !expectedFinal.equals(finalAmount)) {
            throw new RuntimeException("입금 완료 금액 불일치: order.final_price=" + expectedFinal + ", savedPaymentAmount=" + finalAmount);
        }

        // 2) 재고 차감 (여러 상품 락 순서 통일 후 처리)
        List<OrderItem> sortedItemsForFinalize = order.getOrderItems().stream()
                .sorted(ORDER_ITEM_LOCK_ORDER)
                .collect(java.util.stream.Collectors.toList());
        for (OrderItem item : sortedItemsForFinalize) {
            Product product = item.getProduct();
            if (product == null) {
                throw new IllegalStateException("OrderItem에 product가 없습니다. orderId=" + order.getOrderId());
            }
            decreaseStockLocked(product.getId(), item.getSelectedSize(), item.getQuantity(), product.getName());
        }

        // 3) 쿠폰 사용 확정
        if (order.getUsedUserCoupon() != null) {
            Long userCouponId = order.getUsedUserCoupon().getId();
            log.info("[FINALIZE] mark coupon used. userCouponId={}, orderId={}", userCouponId, order.getOrderId());

            Long usedDiscount = userCouponService.markCouponUsedOnSuccess(
                    order.getUser().getId(),
                    userCouponId,
                    order
            );

            if (usedDiscount != null) {
                order.setCoupon_discount(usedDiscount);
            }
        }

        // 4) 포인트 차감
        Long pointsUsed = order.getPoints_discount() == null ? 0L : order.getPoints_discount();
        if (pointsUsed > 0) {
            PointsRequest pointsRequest = new PointsRequest();
            pointsRequest.setUserId(order.getUser().getId());
            pointsRequest.setAmount(pointsUsed);
            pointsRequest.setReason("주문 결제(가상계좌 입금완료)");
            pointsService.usePoints(pointsRequest);
        }

        // 5) 적립금 지급 (할인 전 원가 기준)
        Long baseAmount = order.getOriginal_price();
        if (baseAmount == null || baseAmount <= 0) {
            order.calculateTotalPrice();
            baseAmount = order.getTotal_price() == null ? 0L : order.getTotal_price();
        }
        pointsService.addOrderPoints(order.getUser().getId(), baseAmount);

        // 6) Payment 승인 상태 반영(이미 승인됐다면 멱등)
        Payment p = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalStateException("Payment를 찾을 수 없습니다. paymentKey=" + paymentKey));
        p.approve();
        paymentRepository.save(p);

        // 7) Checkout 삭제 + 장바구니 비우기
        CheckOut checkOut = checkOutService.findByOrderId(order.getOrderId()).orElse(null);
        if (checkOut != null) {
            checkOutService.deleteById(checkOut.getId());
        }
        cartItemRepository.deleteByUser(order.getUser());

        // 8) 주문 상태 ORDERED로 전환 + 최종 금액 저장
        order.setFinal_price(finalAmount);
        order.setTotal_price(finalAmount);
        order.setStatus(Status.ORDERED);

        // 9) 문자 발송(커밋 이후)
        sendOrderCompletedSmsAfterCommit(order.getUser(), order);

        log.info("[FINALIZE] DONE. orderId={}, orderDbId={}, finalAmount={}", order.getOrderId(), order.getId(), finalAmount);
    }   

public JsonNode getTossPaymentByKey(String paymentKey) {
    String tossSecretKey = getTossSecretKeyOrNull();
    if (tossSecretKey == null) {
        throw new IllegalStateException("토스 시크릿 키가 설정되지 않았습니다.");
    }

    String url = "https://api.tosspayments.com/v1/payments/" + paymentKey;

    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth(tossSecretKey, "");
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Void> request = new HttpEntity<>(headers);

    ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            request,
            String.class
    );

    if (!response.getStatusCode().is2xxSuccessful()) {
        throw new RuntimeException("토스 결제 조회 실패: " + response.getBody());
    }

    try {
        return objectMapper.readTree(response.getBody());
    } catch (Exception e) {
        throw new RuntimeException("토스 결제 조회 응답 파싱 실패", e);
    }
}



}
