package com.example.capshop.domain.order;
import com.example.capshop.domain.Product;

import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;

    private Long orderPrice;
    
    private String selectedSize; // 주문 당시 선택한 사이즈 (예: "M", "FREE")

    public OrderItem() {}

    public OrderItem(Product product, int quantity, Long orderPrice) {
        this.product = product;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
    }
    
    public OrderItem(Product product, int quantity, Long orderPrice, String selectedSize) {
        this.product = product;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
        this.selectedSize = selectedSize;
    }

    public Long getSubTotal() {
        return orderPrice * quantity;
    }
}
