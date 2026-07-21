package com.example.capshop.dto;

import java.util.Map;

import com.example.capshop.domain.AuthProvider;


public class UserProfile {
    private final Long id;
    private final String email;
    private final String name;
    private final Boolean isAdmin;
    private final AuthProvider provider; // 타입 변경

    public UserProfile(Map<String, Object> attributes) {
        this.id = attributes.get("id") == null ? null : ((Number) attributes.get("id")).longValue();
        this.email = (String) attributes.get("email");
        this.name = (String) attributes.get("name");
        this.isAdmin = attributes.get("isAdmin") == null ? null : (Boolean) attributes.get("isAdmin");
        String providerStr = (String) attributes.get("provider");
        this.provider = providerStr != null ? AuthProvider.valueOf(providerStr.toUpperCase()) : null; // enum 변환
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public Boolean getIsAdmin() { return isAdmin; }
    public AuthProvider getProvider() { return provider; }
}

