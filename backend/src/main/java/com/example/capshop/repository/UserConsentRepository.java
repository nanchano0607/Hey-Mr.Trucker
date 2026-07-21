package com.example.capshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.capshop.domain.User;
import com.example.capshop.domain.UserConsent;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
    
    // 특정 사용자의 모든 동의 내역 조회
    List<UserConsent> findByUser(User user);
    
    // 특정 사용자의 특정 동의 타입 조회
    List<UserConsent> findByUserAndConsentType(User user, String consentType);
    
    // 특정 사용자의 최신 동의 상태 조회
    @Query("SELECT uc FROM UserConsent uc WHERE uc.user = :user AND uc.consentType = :consentType ORDER BY uc.timestamp DESC")
    Optional<UserConsent> findLatestConsentByUserAndType(@Param("user") User user, @Param("consentType") String consentType);
}
