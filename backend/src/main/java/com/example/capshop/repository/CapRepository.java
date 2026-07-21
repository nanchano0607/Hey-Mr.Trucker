package com.example.capshop.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.Cap;

public interface CapRepository extends JpaRepository<Cap, Long>{
    List<Cap> findByNameContaining(String keyword);
    Optional<Cap> findById(Long id);
    List<Cap> findByIsNewTrue(); // NEW 상품만 조회

}
    

