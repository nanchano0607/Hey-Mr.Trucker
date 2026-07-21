package com.example.capshop.domain.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.capshop.domain.Status;
import com.example.capshop.domain.User;
import com.example.capshop.domain.UserCoupon;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;         // 주문번호 (예: ORD20250131-1)

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(length = 20)
    private Status status;

    // CheckOut에서 복사된 배송 정보
    private String receiverName;    // 수령인
    private String address;         // 배송지 주소
    private String phone;           // 연락처
    private String trackingNumber;  // 송장번호
    private String returnTrackingNumber;  // 반품 송장번호
    
    // 반품 관련 정보
    private String returnReason;    // 반품 사유 (DEFECT: 제품하자, CHANGE_OF_MIND: 단순변심)
    private String returnMethod;    // 반품 방법 (PICKUP: 회수요청, SELF: 직접배송)
    private Long returnShippingFee; // 반품 택배비 (고객 부담액)

    private Long total_price;

    // 할인 정보 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_user_coupon_id")
    private UserCoupon usedUserCoupon;

    private Long original_price = 0L;       // 할인 전 원가
    private Long coupon_discount = 0L;      // 쿠폰 할인액
    private Long points_discount = 0L;      // 포인트 할인액
    private Long total_discount = 0L;       // 총 할인액
    private Long final_price = 0L;          // 최종 결제 금액

    private LocalDateTime orderDate = LocalDateTime.now();
    private LocalDateTime deliveredAt;  // 배송 완료 시각 (반품 기한 계산용)
    
    // 구매확정 관련
    private boolean confirmed = false;           // 구매확정 여부

    private LocalDateTime confirmedAt;           // 구매확정 시간

    // 가상계좌 결제 여부
    @jakarta.persistence.Column(name = "is_virtual_account", nullable = false)
    private boolean isVirtualAccount = false;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    public Order() {}

    public Order(User user) {
        this.user = user;
        this.orderDate = LocalDateTime.now();
    }

    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    public void calculateTotalPrice() {
        this.total_price = orderItems.stream()
                .mapToLong(item -> item.getOrderPrice() * item.getQuantity())
                .sum();
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }

    public boolean isCancellable() {
        return this.status == Status.ORDERED;
    }

    public void deliver() {
        this.status = Status.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void requestReturn() {
        if (!isReturnable()) {
            throw new IllegalStateException("반품 가능 기간이 아닙니다.");
        }
        this.status = Status.RETURN_REQUESTED;
    }

    public boolean isReturnable() {
        // 구매확정된 주문은 반품 불가
        if (this.confirmed) {
            return false;
        }
        
        if (this.status != Status.DELIVERED || this.deliveredAt == null) {
            return false;
        }
        // 배송 완료 후 7일 이내
        return LocalDateTime.now().isBefore(this.deliveredAt.plusDays(7));
    }
    
    // 구매확정
    public void confirmPurchase() {
        if (this.status != Status.DELIVERED && this.status != Status.SHIPPED) {
            throw new IllegalStateException("배송중 또는 배송 완료된 주문만 구매확정할 수 있습니다.");
        }
        if (this.confirmed) {
            throw new IllegalStateException("이미 구매확정된 주문입니다.");
        }
        this.confirmed = true;
        this.confirmedAt = LocalDateTime.now();
    }
    
    // 구매확정 가능 여부 확인 (배송 완료 상태이고 아직 확정 안 된 경우)
    public boolean canConfirm() {
        return (this.status == Status.DELIVERED || this.status == Status.SHIPPED) && !this.confirmed;
    }

    public void approveReturn() {
        if (this.status != Status.RETURN_REQUESTED) {
            throw new IllegalStateException("반품 요청 상태가 아닙니다.");
        }
        this.status = Status.RETURN_SHIPPING;
    }

    public void completeReturn() {
        if (this.status != Status.RETURN_SHIPPING) {
            throw new IllegalStateException("반품 배송중 상태가 아닙니다.");
        }
        this.status = Status.RETURNED;
    }

    public void cancelReturn() {
        if (this.status != Status.RETURN_REQUESTED) {
            throw new IllegalStateException("반품 요청 상태가 아닙니다.");
        }
        this.status = Status.DELIVERED;
    }

    public void ship() {
        if (this.status != Status.PREPARING_SHIPMENT) {
            throw new IllegalStateException("배송준비중 상태에서만 배송을 시작할 수 있습니다.");
        }
        this.status = Status.SHIPPED;
    }

    public void markAsDelivered() {
        if (this.status != Status.SHIPPED) {
            throw new IllegalStateException("배송중 상태에서만 배송 완료 처리할 수 있습니다.");
        }
        this.status = Status.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void applyCouponDiscount(long discount, UserCoupon uc) {
        this.usedUserCoupon = uc;                 // 어떤 쿠폰을 썼는지만 연결
        this.coupon_discount = Math.max(0L, discount);
        long points = this.points_discount == null ? 0L : this.points_discount;
        this.total_discount = points + this.coupon_discount;

        long base = (this.original_price != null && this.original_price > 0)
                ? this.original_price
                : (this.total_price == null ? 0L : this.total_price);
        this.final_price = Math.max(0L, base - this.total_discount);
    }

    public boolean isVirtualAccount() {
        return isVirtualAccount;
    }

    public void setVirtualAccount(boolean virtualAccount) {
        isVirtualAccount = virtualAccount;
    }
}
