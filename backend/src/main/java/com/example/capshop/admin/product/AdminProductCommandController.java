package com.example.capshop.admin.product;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.product.ProductType;
import com.example.capshop.domain.product.VintageCategory;
import com.example.capshop.service.product.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 등록/삭제/신상품 지정/재고·가격 수정 (관리자 전용).
 * 캡·액세서리·빈티지는 모두 Product 로 저장되며 경로의 {type} 으로 ProductType 이 결정된다.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/admin")
public class AdminProductCommandController {

    private final ProductService productService;

    @PostMapping("/{type:cap|acc|vintage}/save")
    public void save(@PathVariable("type") String type, @RequestBody Map<String, Object> requestData) {
        ProductType productType = ProductType.valueOf(type.toUpperCase(Locale.ROOT));

        Product product = new Product();
        product.setName((String) requestData.get("name"));
        product.setPrice(Long.valueOf(requestData.get("price").toString()));
        product.setColor((String) requestData.get("color"));
        product.setSizeInfo((String) requestData.get("sizeInfo"));
        product.setMainImageUrl((String) requestData.get("mainImageUrl"));
        product.setProductType(productType);
        if (productType == ProductType.VINTAGE) {
            product.setVintageCategory(parseVintageCategory(requestData.get("vintageCategory")));
        }

        @SuppressWarnings("unchecked")
        List<String> sizes = (List<String>) requestData.get("size");
        product.setSize(sizes);

        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) requestData.get("imageUrls");
        product.setImageUrls(imageUrls);

        Product saved = productService.save(product);

        @SuppressWarnings("unchecked")
        Map<String, Object> sizeStocks = (Map<String, Object>) requestData.get("sizeStocks");
        if (sizeStocks != null) {
            for (Map.Entry<String, Object> entry : sizeStocks.entrySet()) {
                productService.updateStockBySize(saved.getId(), entry.getKey(), Long.valueOf(entry.getValue().toString()));
            }
        }
    }

    @PostMapping("/{type:cap|acc|vintage}/delete/{id}")
    public void delete(@PathVariable("id") Long id) {
        productService.deleteById(id);
    }

    @PostMapping("/{type:cap|acc|vintage|product}/setNew/{id}")
    public void setAsNew(@PathVariable("id") Long id) {
        productService.setIsNew(id, true);
    }

    @PostMapping("/{type:cap|acc|vintage|product}/unsetNew/{id}")
    public void unsetAsNew(@PathVariable("id") Long id) {
        productService.setIsNew(id, false);
    }

    @PostMapping("/{type:cap|acc|vintage}/updateStock/{id}")
    public void updateStock(@PathVariable("id") Long id, @RequestBody Long stock) {
        productService.updateStock(id, stock);
    }

    @PostMapping("/{type:cap|acc|vintage}/updateStock/{id}/{size}")
    public void updateStockBySize(
            @PathVariable("id") Long id,
            @PathVariable("size") String size,
            @RequestBody Long stock) {
        log.info("[UPDATE_STOCK] id={}, size={}, stock={}", id, size, stock);
        productService.updateStockBySize(id, size, stock);
    }

    @PutMapping("/product/{id}/price")
    public ResponseEntity<Product> updatePrice(@PathVariable("id") Long id, @RequestBody Long price) {
        Product product = productService.findById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        // 현재 가격을 pastPrice에 저장 (세일 시 이전 가격 보관)
        if (product.getPrice() != null && !product.getPrice().equals(price)) {
            product.setPastPrice(product.getPrice());
        }
        product.setPrice(price);
        return ResponseEntity.ok(productService.save(product));
    }

    private VintageCategory parseVintageCategory(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        String value = rawValue.toString().trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            return VintageCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown vintageCategory received: {}", value);
            return null;
        }
    }
}
