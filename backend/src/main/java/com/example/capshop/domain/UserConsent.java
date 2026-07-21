package com.example.capshop.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class UserConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String consentType; // 동의 타입 (예: TERMS, PRIVACY, MARKETING_EMAIL 등)
    private String version = "v1.0";      // 동의 항목 버전 (초기값)
    private boolean agreed;      // 동의 여부
    private Instant timestamp;   // 동의/철회 시각
    private String ip;           // 동의 당시 IP 주소
    private String userAgent;    // 동의 당시 브라우저 정보
}
