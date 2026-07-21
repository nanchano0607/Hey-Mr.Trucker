package com.example.capshop.repository;

import com.example.capshop.domain.Logbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LogbookRepository extends JpaRepository<Logbook, Long> {
    // 기본적으로 엔티티에 맞는 간단한 조회 메서드들만 두었습니다.
    List<Logbook> findAllByOrderByIdDesc();

@Query("SELECT MAX(l.sortOrder) FROM Logbook l")
Integer findMaxSortOrder();
}
