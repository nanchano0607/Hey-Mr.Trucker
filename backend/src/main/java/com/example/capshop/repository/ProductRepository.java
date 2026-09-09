package com.example.capshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.ProductType;
import com.example.capshop.domain.ProductStatus;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 기본 목록 조회는 JpaRepository.findAll() 사용 가능
    List<Product> findByProductType(ProductType type);

    // CapRepository와 동일한 편의 메서드들
    List<Product> findByNameContaining(String keyword);
    java.util.Optional<Product> findById(Long id);
    List<Product> findByIsNewTrue();

    // 사이즈 구분 없는(ONE SIZE) 상품의 Product.stock 증감 시 동시성 제어용 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    java.util.Optional<Product> findByIdForUpdate(@Param("id") Long id);

    // ✅ Soft delete: status가 NULL(기존 데이터) 또는 ACTIVE 인 상품만 노출
    @Query("select p from Product p where p.status is null or p.status = :status")
    List<Product> findAllByStatusOrNull(@Param("status") ProductStatus status);

    @Query("select p from Product p where p.isNew = true and (p.status is null or p.status = :status)")
    List<Product> findNewByStatusOrNull(@Param("status") ProductStatus status);

    @Query("select p from Product p where p.name like %:keyword% and (p.status is null or p.status = :status)")
    List<Product> findByNameContainingAndStatusOrNull(@Param("keyword") String keyword, @Param("status") ProductStatus status);

   @Query("select img from Product p join p.imageUrls img where p.id = :productId")
    List<String> findImageUrlsByProductId(@Param("productId") Long productId);
}


