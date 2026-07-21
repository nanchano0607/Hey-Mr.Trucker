package com.example.capshop.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class CapStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cap_id")
    @JsonBackReference
    private Cap cap;

    private String size;  // 사이즈 (예: "S", "M", "L", "XL", "FREE")
    
    private Long stock;   // 해당 사이즈의 재고 수량

    public CapStock(Cap cap, String size, Long stock) {
        this.cap = cap;
        this.size = size;
        this.stock = stock;
    }
    
    // 재고 감소
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalStateException("재고가 부족합니다. (현재 재고: " + this.stock + ")");
        }
        this.stock -= quantity;
    }
    
    // 재고 증가
    public void increaseStock(int quantity) {
        this.stock += quantity;
    }
}