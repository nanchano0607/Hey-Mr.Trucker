package com.example.capshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.ProductStock;

import jakarta.persistence.LockModeType;

public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {
    List<ProductStock> findByProduct(Product product);
    List<ProductStock> findByProduct_Id(Long productId);
    Optional<ProductStock> findByProductAndSize(Product product, String size);

    // 재고 증감 시 동시성 제어용: 해당 사이즈 재고 행에 비관적 락(FOR UPDATE)을 건다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ps from ProductStock ps where ps.product.id = :productId and ps.size = :size")
    Optional<ProductStock> findByProductIdAndSizeForUpdate(@Param("productId") Long productId, @Param("size") String size);
}
