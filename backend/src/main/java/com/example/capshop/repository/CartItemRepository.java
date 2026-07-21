package com.example.capshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.CartItem;
import com.example.capshop.domain.User;

public interface CartItemRepository extends JpaRepository<CartItem, Long>{
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    List<CartItem> findAllByUserAndProduct(User user, Product product);  // 리스트 반환용 메서드 추가
    Optional<CartItem> findByUserAndProductAndSize(User user, Product product, String size);
    @Modifying
    @Query("delete from CartItem ci where ci.user = :user")
    void deleteByUser(@Param("user") User user);
    @Modifying
    @Query("delete from CartItem ci where ci.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
