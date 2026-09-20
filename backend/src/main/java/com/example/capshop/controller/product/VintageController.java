package com.example.capshop.controller.product;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.product.ProductStock;
import com.example.capshop.domain.product.ProductType;
import com.example.capshop.service.product.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vintage")
public class VintageController {
    private final ProductService productService;

    @GetMapping("/{id}")
    public Product vintageDetail(@PathVariable("id") Long id){
        return productService.findById(id);
    }

    @GetMapping("/findAll")
    public List<Product> findAll(){
        return productService.findAllActive().stream()
                .filter(p -> p.getProductType() == ProductType.VINTAGE)
                .toList();
    }

    @GetMapping("/new")
    public List<Product> findNewVintages(){
        return productService.findNewProducts();
    }

    // 특정 상품의 모든 사이즈별 재고 조회
    @GetMapping("/stocks/{id}")
    public java.util.Map<String, Long> getStocksByVintageId(@PathVariable("id") Long id) {
        Product product = productService.findById(id);
        java.util.Map<String, Long> stockMap = new java.util.HashMap<>();

        if (product != null && product.getStocks() != null) {
            for (ProductStock stock : product.getStocks()) {
                stockMap.put(stock.getSize(), stock.getStock());
            }
        }

        return stockMap;
    }

    @GetMapping("/getImages/{id}")
    public List<String> getImages(@PathVariable("id") Long id) {
        Product product = productService.findById(id);
        List<String> filenames = new ArrayList<>();
        if (product == null) return filenames;
        if (product.getMainImageUrl() != null) {
            filenames.add(product.getMainImageUrl().substring(product.getMainImageUrl().lastIndexOf("/") + 1));
        }
        if (product.getImageUrls() != null) {
            for (String url : product.getImageUrls()) {
                filenames.add(url.substring(url.lastIndexOf("/") + 1));
            }
        }
        return filenames;
    }

}
