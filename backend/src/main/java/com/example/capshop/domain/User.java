package com.example.capshop.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements UserDetails {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String name;
    private String password;
    private boolean isAdmin = false;

    @Column(unique = true)
    private String phone;           // 전화번호
    
    @ElementCollection
    @CollectionTable(name = "user_address", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "address")
    private List<String> address;  // 배송지 목록
    
    private String zipcode;         // 우편번호

    private LocalDateTime createdAt;   // 가입일
    private LocalDateTime updatedAt;   // 수정일

    private boolean isDeleted = false;      // 탈퇴 여부 (isDeleted -> deleted로 변경)
    
    private Long points = 0L;              // 적립금 (원 단위)
    
    @Enumerated(EnumType.STRING)
    private AuthProvider oauthProvider;   // OAuth 타입 (예: GOOGLE, KAKAO, NONE)
    private String providerUserId;       // 소셜 플랫폼에서 내려주는 고유 ID

    private String gender;          // 성별
    private LocalDate birth; // 생년월일

    @Builder
    public User(String email, String password, String name,
                boolean isAdmin,
                String phone, List<String> address, String zipcode,
                LocalDateTime createdAt, LocalDateTime updatedAt,
                boolean deleted, Long points, AuthProvider oauthProvider,
                String providerUserId,
                String gender, LocalDate birth) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.isAdmin = isAdmin;
        this.phone = phone;
        this.address = address;
        this.zipcode = zipcode;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.isDeleted = deleted;
        this.points = points != null ? points : 0L;
        this.oauthProvider = oauthProvider;
        this.providerUserId = providerUserId;
        this.gender = gender;
        this.birth = birth;
    }

    public User update(String name){
        this.name = name;
        return this;
    }
    
    // 적립금 관련 메서드들
    public void addPoints(Long amount) {
        if (amount > 0) {
            this.points = (this.points != null ? this.points : 0L) + amount;
        }
    }
    
    public void usePoints(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용할 적립금은 0보다 커야 합니다.");
        }
        if (this.points == null || this.points < amount) {
            throw new IllegalArgumentException("보유 적립금이 부족합니다.");
        }
        this.points -= amount;
    }
    
    public Long getAvailablePoints() {
        return this.points != null ? this.points : 0L;
    }


    @OneToMany(mappedBy = "user")
        private List<UserConsent> consents;



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (isAdmin) {
            return List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    
}
