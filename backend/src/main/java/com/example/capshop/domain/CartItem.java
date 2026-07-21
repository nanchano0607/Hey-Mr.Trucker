package com.example.capshop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;  // User 엔티티 참조

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;    // Product 엔티티 참조 (DB 컬럼명은 cap_id 유지)

    private int quantity;
    
    private String size;  // 선택된 사이즈 (S, M, L, XL 등)

    public CartItem() {
    }

    public CartItem(User user, Product product, int quantity, String size) {
        this.user = user;
        this.product = product;
        this.quantity = quantity;
        this.size = size;
    }
    
    // 기존 생성자 유지 (하위 호환성)
    public CartItem(User user, Product product, int quantity) {
        this(user, product, quantity, null);
    }
}
