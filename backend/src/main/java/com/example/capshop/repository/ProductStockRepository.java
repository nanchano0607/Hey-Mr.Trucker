package com.example.capshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.ProductStock;

public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {
    List<ProductStock> findByProduct(Product product);
    List<ProductStock> findByProduct_Id(Long productId);
    Optional<ProductStock> findByProductAndSize(Product product, String size);
}
