package com.example.capshop.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.capshop.domain.User;
import com.example.capshop.domain.UserConsent;

import lombok.Getter;

@Getter
public class UserAdminResponse {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private boolean admin;
    private boolean deleted;
    private LocalDateTime createdAt;
    private String oauthProvider;
    private List<UserConsentResponse> consents;

    public UserAdminResponse(User u) {
        this.id = u.getId();
        this.email = u.getEmail();
        this.name = u.getName();
        this.phone = u.getPhone();
        this.admin = u.isAdmin();
        this.deleted = !u.isDeleted();
        this.createdAt = u.getCreatedAt();
        this.oauthProvider = u.getOauthProvider() != null ? u.getOauthProvider().name() : null;
        if (u.getConsents() == null || u.getConsents().isEmpty()) {
            this.consents = List.of();
            return;
        }

        // 동일 consentType이 여러 번 저장되는 구조(히스토리)라서, 응답에는 최신 상태만 내려줌
        Comparator<UserConsent> byTimestamp = Comparator.comparing(UserConsent::getTimestamp,
            Comparator.nullsFirst(Comparator.naturalOrder()));
        Map<String, UserConsent> latestByType = u.getConsents().stream()
            .filter(c -> c.getConsentType() != null)
            .collect(Collectors.toMap(
                UserConsent::getConsentType,
                Function.identity(),
                (a, b) -> byTimestamp.compare(a, b) >= 0 ? a : b));

        this.consents = latestByType.values().stream()
            .sorted(byTimestamp.reversed())
            .map(UserConsentResponse::new)
            .toList();
    }
}
