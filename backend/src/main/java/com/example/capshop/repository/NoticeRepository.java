package com.example.capshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByCreatedAtDesc();
}
