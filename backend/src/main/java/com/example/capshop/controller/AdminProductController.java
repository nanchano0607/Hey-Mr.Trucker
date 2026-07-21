package com.example.capshop.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.ProductType;
import com.example.capshop.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    /** 전체 상품(활성/비활성 포함) */
    @GetMapping
    public List<Product> findAll() {
        return productService.findAllIncludingInactive();
    }

    /** 타입별 전체(활성/비활성 포함) */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Product>> findAllByType(@PathVariable("type") ProductType type) {
        return ResponseEntity.ok(productService.findAllByTypeIncludingInactive(type));
    }

    /** 단건 조회(활성/비활성 포함) */
    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable("id") Long id) {
        Product p = productService.findById(id);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    /** 판매 재개 (ACTIVE) */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable("id") Long id) {
        productService.activateById(id);
        return ResponseEntity.noContent().build();
    }

    /** 판매 중지 (INACTIVE) - soft delete */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable("id") Long id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
