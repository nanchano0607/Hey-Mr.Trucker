package com.example.capshop.repository.content;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.content.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByCreatedAtDesc();
}
