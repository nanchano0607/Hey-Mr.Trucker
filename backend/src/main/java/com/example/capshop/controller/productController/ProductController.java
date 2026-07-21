package com.example.capshop.controller.productController;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.Product;
import com.example.capshop.service.ProductService;

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
    @PostMapping("/product/setNew/{id}")
    public void setProductAsNew(@PathVariable("id") Long id){
        productService.setIsNew(id, true);
    }

    @PostMapping("/product/unsetNew/{id}")
    public void unsetProductAsNew(@PathVariable("id") Long id){
        productService.setIsNew(id, false);
    }
  @GetMapping("/product/image_urls/{id}")
public ResponseEntity<List<String>> getImageUrls(@PathVariable("id") Long id) {
    System.out.println("상품이미지 출력");
    List<String> imageUrls = productService.getProductImages(id);
    System.out.println(imageUrls);
    return ResponseEntity.ok(imageUrls);
}

    @PutMapping("/product/{id}/price")
    public ResponseEntity<Product> updatePrice(
            @PathVariable("id") Long id, 
            @RequestBody Long price) {
        Product product = productService.findById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        // 현재 가격을 pastPrice에 저장 (세일 시 이전 가격 보관)
        if (product.getPrice() != null && !product.getPrice().equals(price)) {
            product.setPastPrice(product.getPrice());
        }
        product.setPrice(price);
        Product updated = productService.save(product);
        return ResponseEntity.ok(updated);
    }
}
