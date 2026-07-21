package com.example.capshop.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.capshop.domain.order.Order;

import lombok.Getter;

@Getter
public class OrderResponse {
    private Long id;
    private String orderId;         // 주문번호 (예: ORD20250131-1)
    private String status;
    private String receiverName;
    private String address;
    private String phone;
    private Long totalPrice;
    
    // 할인 정보 추가
    private Long originalPrice;     // 할인 전 원가
    private Long couponDiscount;    // 쿠폰 할인액
    private Long pointsDiscount;    // 포인트 할인액
    private Long totalDiscount;     // 총 할인액
    private Long finalPrice;        // 최종 결제 금액
    
    private LocalDateTime orderDate;
    private String trackingNumber;
    private String returnTrackingNumber;
    private String returnReason;
    private String returnMethod;
    private Long returnShippingFee;
    private boolean confirmed;           // 구매확정 여부
    private LocalDateTime confirmedAt;   // 구매확정 시간
    private LocalDateTime deliveredAt;   // 배송 완료 시간
    private List<OrderItemDto> orderItems;
    private boolean isVirtualAccount;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.orderId = order.getOrderId();
        this.status = order.getStatus().name();
        this.receiverName = order.getReceiverName();
        this.address = order.getAddress();
        this.phone = order.getPhone();
        this.totalPrice = order.getTotal_price();
        
        // 할인 정보 매핑
        this.originalPrice = order.getOriginal_price();
        this.couponDiscount = order.getCoupon_discount();
        this.pointsDiscount = order.getPoints_discount();
        this.totalDiscount = order.getTotal_discount();
        this.finalPrice = order.getFinal_price();
        
        this.orderDate = order.getOrderDate();
        this.trackingNumber = order.getTrackingNumber();
        this.returnTrackingNumber = order.getReturnTrackingNumber();
        this.returnReason = order.getReturnReason();
        this.returnMethod = order.getReturnMethod();
        this.returnShippingFee = order.getReturnShippingFee();
        this.confirmed = order.isConfirmed();
        this.confirmedAt = order.getConfirmedAt();
        this.deliveredAt = order.getDeliveredAt();
        this.isVirtualAccount = order.isVirtualAccount();
        this.orderItems = order.getOrderItems().stream()
                .map(OrderItemDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class OrderItemDto {
        private Long id;
        private Long productId;
        private String productName;
        private String productType;
        private String vintageCategory;
        private int quantity;
        private Long orderPrice;
        private Long subTotal;
        private String selectedSize;

        public OrderItemDto(com.example.capshop.domain.order.OrderItem item) {
            this.id = item.getId();
            this.productId = item.getProduct().getId();
            this.productName = item.getProduct().getName();
            this.productType = item.getProduct().getProductType() != null
                    ? item.getProduct().getProductType().name()
                    : null;
            this.vintageCategory = item.getProduct().getVintageCategory() != null
                    ? item.getProduct().getVintageCategory().name()
                    : null;
            this.quantity = item.getQuantity();
            this.orderPrice = item.getOrderPrice();
            this.subTotal = item.getSubTotal();
            this.selectedSize = item.getSelectedSize();
        }
    }
}
