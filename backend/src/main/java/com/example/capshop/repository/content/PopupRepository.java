package com.example.capshop.repository.content;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.content.Popup;

public interface PopupRepository extends JpaRepository<Popup, Long> {
    Optional<Popup> findTopByOrderByIdAsc();
}
