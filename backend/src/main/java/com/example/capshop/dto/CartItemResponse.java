package com.example.capshop.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CartItemResponse {
    private Long id;
    private int quantity;
    private Long productId;
    private String productName;
    private Long price;
    private String mainImageUrl;
    private String size;  // 선택된 사이즈 추가

    public CartItemResponse(Long id, int quantity, Long productId, String productName, Long price, String mainImageUrl, String size) {
        this.id = id;
        this.quantity = quantity;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.mainImageUrl = mainImageUrl;
        this.size = size;
    }
    
    // 기존 생성자 유지 (하위 호환성)
    public CartItemResponse(Long id, int quantity, Long productId, String productName, Long price, String mainImageUrl) {
        this(id, quantity, productId, productName, price, mainImageUrl, null);
    }
}
