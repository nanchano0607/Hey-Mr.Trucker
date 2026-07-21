package com.example.capshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.Cap;
import com.example.capshop.domain.CapStock;

public interface CapStockRepository extends JpaRepository<CapStock, Long> {
    List<CapStock> findByCap(Cap cap);
    Optional<CapStock> findByCapAndSize(Cap cap, String size);
    void deleteByCapAndSize(Cap cap, String size);
}