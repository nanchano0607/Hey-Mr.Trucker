package com.example.capshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.Popup;

public interface PopupRepository extends JpaRepository<Popup, Long> {
    Optional<Popup> findTopByOrderByIdAsc();
}
