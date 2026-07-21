package com.example.capshop.dto;

import java.time.Instant;

import com.example.capshop.domain.UserConsent;

import lombok.Getter;

@Getter
public class UserConsentResponse {
    private String consentType;
    private String version;
    private boolean agreed;
    private Instant timestamp;

    public UserConsentResponse(UserConsent consent) {
        this.consentType = consent.getConsentType();
        this.version = consent.getVersion();
        this.agreed = consent.isAgreed();
        this.timestamp = consent.getTimestamp();
    }
}
