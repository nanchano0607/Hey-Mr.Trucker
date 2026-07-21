package com.example.capshop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddCartItemRequest {
    private Long userId;
    private Long productId;
    private int quantity;
    private String size;  // 선택된 사이즈 추가
    
}
