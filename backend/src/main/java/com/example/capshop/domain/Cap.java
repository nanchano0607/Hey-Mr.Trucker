package com.example.capshop.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Cap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Long price;
    private String description;//상품설명
    private Long stock;//기존 재고 (하위 호환성)
    private String color; // 색상
    
    @OneToMany(mappedBy = "cap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CapStock> stocks; // 사이즈별 재고
    
    @ElementCollection
    private List<String> size; // 사이즈 목록 (예: ["S", "M", "L"] 또는 ["FREE"])
    
    @Column(columnDefinition = "TEXT")
    private String sizeInfo; // 사이즈 상세 정보 (예: 둘레 55-58cm, 챙 길이 7cm, 깊이 12cm)
    
    private String mainImageUrl; // 대표 이미지 URL
    
    @ElementCollection
    private List<String> imageUrls; // 추가 이미지 URL 목록
    
    @Column(nullable = false)
    private Boolean isNew = false; // 신상품 여부 (관리자가 설정)
    
    // 특정 사이즈의 재고 조회
    public Long getStockBySize(String size) {
        if (stocks == null) return 0L;
        return stocks.stream()
                .filter(stock -> stock.getSize().equals(size))
                .findFirst()
                .map(CapStock::getStock)
                .orElse(0L);
    }
    
    // 전체 재고 합계 (하위 호환성을 위한 오버라이드)
    public Long getStock() {
        if (stocks == null || stocks.isEmpty()) {
            return this.stock; // 기존 stock 필드 반환
        }
        return stocks.stream()
                .mapToLong(CapStock::getStock)
                .sum();
    }
    
    // 특정 사이즈의 CapStock 객체 조회
    public CapStock getCapStockBySize(String size) {
        if (stocks == null) return null;
        return stocks.stream()
                .filter(stock -> stock.getSize().equals(size))
                .findFirst()
                .orElse(null);
    }
    
}
