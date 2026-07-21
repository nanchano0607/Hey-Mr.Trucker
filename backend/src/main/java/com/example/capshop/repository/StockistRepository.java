package com.example.capshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.Stockist;

public interface StockistRepository extends JpaRepository<Stockist, Long> {
    List<Stockist> findAllByOrderByIdDesc();
}
