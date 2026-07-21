package com.example.capshop.controller.productController;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.ProductStock;
import com.example.capshop.domain.ProductType;
import com.example.capshop.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/acc")
public class AccController {
    private final ProductService productService;

    @PostMapping("/save")
    public void saveAcc(@RequestBody java.util.Map<String, Object> requestData){
        // Acc를 Product로 저장 (productType을 ACC으로 설정)
        Product product = new Product();
        product.setName((String) requestData.get("name"));
        product.setPrice(Long.valueOf(requestData.get("price").toString()));
        product.setColor((String) requestData.get("color"));
        product.setSizeInfo((String) requestData.get("sizeInfo"));
        product.setMainImageUrl((String) requestData.get("mainImageUrl"));
        product.setProductType(ProductType.ACC);

        @SuppressWarnings("unchecked")
        List<String> sizes = (List<String>) requestData.get("size");
        product.setSize(sizes);

        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) requestData.get("imageUrls");
        product.setImageUrls(imageUrls);

        Product saved = productService.save(product);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> sizeStocks = (java.util.Map<String, Object>) requestData.get("sizeStocks");
        if (sizeStocks != null && !sizeStocks.isEmpty()) {
            for (java.util.Map.Entry<String, Object> entry : sizeStocks.entrySet()) {
                String size = entry.getKey();
                Long stock = Long.valueOf(entry.getValue().toString());
                productService.updateStockBySize(saved.getId(), size, stock);
            }
        }
    }

    @PostMapping("/delete/{id}")
    public void deleteAcc(@PathVariable("id") Long id){
        productService.deleteById(id);
    }

    @GetMapping("/{id}")
    public Product accDetail(@PathVariable("id") Long id){
        return productService.findById(id);
    }

    @GetMapping("/findAll")
    public List<Product> findAll(){
        return productService.findAllActive().stream()
                .filter(p -> p.getProductType() == ProductType.ACC)
                .toList();
    }

    @GetMapping("/new")
    public List<Product> findNewAccs(){
        return productService.findNewProducts();
    }

    @PostMapping("/setNew/{id}")
    public void setAccAsNew(@PathVariable("id") Long id){
        productService.setIsNew(id, true);
    }

    @PostMapping("/unsetNew/{id}")
    public void unsetAccAsNew(@PathVariable("id") Long id){
        productService.setIsNew(id, false);
    }

    @PostMapping("/updateStock/{id}")
    public void updateStock(@PathVariable("id") Long id, @RequestBody Long stock) {
        productService.updateStock(id, stock);
    }

    // 사이즈별 재고 업데이트
    @PostMapping("/updateStock/{id}/{size}")
    public void updateStockBySize(
            @PathVariable("id") Long id, 
            @PathVariable("size") String size, 
            @RequestBody Long stock) {
        productService.updateStockBySize(id, size, stock);
    }

    // 특정 상품의 모든 사이즈별 재고 조회
    @GetMapping("/stocks/{id}")
    public java.util.Map<String, Long> getStocksByAccId(@PathVariable("id") Long id) {
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

