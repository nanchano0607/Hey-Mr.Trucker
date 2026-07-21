package com.example.capshop.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long price;
    
    private Long pastPrice; // 이전 가격 (세일 시 사용)

    private Long stock; // 기존 재고 (하위 호환성)

    private String color; // 색상

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ProductStock> stocks; // 사이즈별 재고

    @ElementCollection
    private List<String> size; // 사이즈 목록

    @Column(columnDefinition = "TEXT")
    private String sizeInfo; // 사이즈 상세 정보

    private String mainImageUrl; // 대표 이미지 URL

    @ElementCollection
    private List<String> imageUrls; // 추가 이미지 URL 목록

    @Column(nullable = false)
    private Boolean isNew = false; // 신상품 여부

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private VintageCategory vintageCategory;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ACTIVE;


    // 특정 사이즈의 재고 조회
    public Long getStockBySize(String size) {
        if (stocks == null) return 0L;
        return stocks.stream()
                .filter(s -> s.getSize().equals(size))
                .findFirst()
                .map(ProductStock::getStock)
                .orElse(0L);
    }

    // 전체 재고 합계 (하위 호환성을 위한 오버라이드)
    public Long getStock() {
        if (stocks == null || stocks.isEmpty()) {
            return this.stock;
        }
        return stocks.stream()
                .mapToLong(ProductStock::getStock)
                .sum();
    }

    // 특정 사이즈의 ProductStock 객체 조회
    public ProductStock getProductStockBySize(String size) {
        if (stocks == null) return null;
        return stocks.stream()
                .filter(s -> s.getSize().equals(size))
                .findFirst()
                .orElse(null);
    }

    // 날짜 필드 및 자동 설정 제거 (요청에 따라)
}
