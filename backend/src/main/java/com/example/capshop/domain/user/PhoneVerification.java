package com.example.capshop.domain.user;

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

    private LocalDateTime verifiedAt; // 인증 성공 시각 (유효 시간 판단용)

    private int attemptCount = 0; // 인증시도 횟수 제한

    private LocalDateTime lastSentAt; // 최근 발송 시간 보호용

    @Builder
    public PhoneVerification(String phoneNumber, String code, LocalDateTime createdAt,
                             LocalDateTime expiresAt, boolean verified, LocalDateTime verifiedAt, int attemptCount,
                             LocalDateTime lastSentAt) {
        this.phoneNumber = phoneNumber;
        this.code = code;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.verified = verified;
        this.verifiedAt = verifiedAt;
        this.attemptCount = attemptCount;
        this.lastSentAt = lastSentAt;
    }

    /** 인증 성공 후 이 시간(분) 안에서만 가입·아이디 찾기·비밀번호 재설정에 쓸 수 있다. */
    public static final int VERIFIED_VALID_MINUTES = 10;

    public void markVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    /** 인증이 끝났고 유효 시간 안일 때만 사용할 수 있다. 인증 시각이 없는 과거 데이터는 사용할 수 없다. */
    public boolean isUsable(LocalDateTime now) {
        return verified && verifiedAt != null && now.isBefore(verifiedAt.plusMinutes(VERIFIED_VALID_MINUTES));
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
