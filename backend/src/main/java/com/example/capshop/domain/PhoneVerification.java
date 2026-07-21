package com.example.capshop.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "phone_verification")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(length = 10, nullable = false)
    private String code; // 6자리 인증 코드

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private boolean verified = false; // 인증 성공 여부

    private int attemptCount = 0; // 인증시도 횟수 제한

    private LocalDateTime lastSentAt; // 최근 발송 시간 보호용

    @Builder
    public PhoneVerification(String phoneNumber, String code, LocalDateTime createdAt,
                             LocalDateTime expiresAt, boolean verified, int attemptCount,
                             LocalDateTime lastSentAt) {
        this.phoneNumber = phoneNumber;
        this.code = code;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.verified = verified;
        this.attemptCount = attemptCount;
        this.lastSentAt = lastSentAt;
    }

    public void markVerified() {
        this.verified = true;
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
