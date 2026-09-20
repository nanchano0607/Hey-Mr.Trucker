package com.example.capshop.controller.product;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.product.Product;
import com.example.capshop.service.product.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;

    @GetMapping("/product/findAll")
    public List<Product> findAll() {
        return productService.findAllActive();
    }
        @GetMapping("/product/new")
    public List<Product> findNewproducts(){
        return productService.findNewProducts();
    }
  @GetMapping("/product/image_urls/{id}")
public ResponseEntity<List<String>> getImageUrls(@PathVariable("id") Long id) {
    System.out.println("상품이미지 출력");
    List<String> imageUrls = productService.getProductImages(id);
    System.out.println(imageUrls);
    return ResponseEntity.ok(imageUrls);
}
}
