package com.example.capshop.repository.content;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.content.Stockist;

public interface StockistRepository extends JpaRepository<Stockist, Long> {
    List<Stockist> findAllByOrderByIdDesc();
}
