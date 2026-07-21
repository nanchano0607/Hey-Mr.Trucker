package com.example.capshop.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.ProductStatus;
import com.example.capshop.domain.ProductType;
import com.example.capshop.repository.CartItemRepository;
import com.example.capshop.repository.ProductRepository;
import com.example.capshop.repository.ProductStockRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ProductService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;

    public Product save(Product product){
        return productRepository.save(product);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /** 사용자 노출용: ACTIVE(+NULL=기존 데이터)만 */
    public List<Product> findAllActive() {
        return productRepository.findAllByStatusOrNull(ProductStatus.ACTIVE);
    }

    /** 관리자용: ACTIVE/INACTIVE 전체 */
    public List<Product> findAllIncludingInactive() {
        return productRepository.findAll();
    }

    public List<Product> findAllByTypeIncludingInactive(ProductType type) {
        return productRepository.findByProductType(type);
    }
    
    @Transactional
    public void deleteById(Long id) {
        // ✅ 물리 삭제 금지: 판매중지(INACTIVE) 처리
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return;

        // 장바구니에 남아있으면 UX가 깨지므로 제거(주문 기록은 건드리지 않음)
        try {
            cartItemRepository.deleteByProductId(id);
        } catch (Exception ignore) {}

        p.setStatus(ProductStatus.INACTIVE);
        productRepository.save(p);
    }

    public List<Product> findByName(String keyword) {
        return productRepository.findByNameContaining(keyword);
    }

    public List<Product> findByNameActive(String keyword) {
        return productRepository.findByNameContainingAndStatusOrNull(keyword, ProductStatus.ACTIVE);
    }

    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null); // 없으면 null
    }

    /** 관리자용: 비활성 상품도 다시 활성화 */
    @Transactional
    public void activateById(Long id) {
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return;
        p.setStatus(ProductStatus.ACTIVE);
        productRepository.save(p);
    }
    
    public List<Product> findNewProducts() {
        // 사용자 노출용: ACTIVE(+NULL=기존 데이터)만
        return productRepository.findNewByStatusOrNull(ProductStatus.ACTIVE);
    }
    
    public void setIsNew(Long id, boolean isNew) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            product.setIsNew(isNew);
            productRepository.save(product);
        }
    }
    
    // 사이즈별 재고 업데이트
    public void updateStockBySize(Long productId, String size, Long stock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));
        
        com.example.capshop.domain.ProductStock productStock = product.getProductStockBySize(size);
        if (productStock == null) {
            productStock = new com.example.capshop.domain.ProductStock(product, size, stock);
            if (product.getStocks() == null) {
                product.setStocks(new java.util.ArrayList<>());
            }
            product.getStocks().add(productStock);
        } else {
            productStock.setStock(stock);
        }
        
        productRepository.save(product);
    }
    
    // 기존 메서드 유지 (하위 호환성)
    public void updateStock(Long id, Long stock) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + id));
        product.setStock(stock);
        productRepository.save(product);
    }
    public List<String> getProductImages(Long id) {
    // 상품 존재 검증
    productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다."));

    return productRepository.findImageUrlsByProductId(id);
}

    @Transactional
    public void updateMainImageUrl(Long productId, String mainImageUrl) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));
        product.setMainImageUrl(mainImageUrl);
        productRepository.save(product);
    }

    @Transactional
    public void updateImageUrls(Long productId, List<String> imageUrls) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));
        product.setImageUrls(imageUrls);
        productRepository.save(product);
    }
}
