package com.example.capshop.service;

import java.util.List;
import java.util.Optional;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.capshop.domain.AuthProvider;
import com.example.capshop.domain.User;
import com.example.capshop.domain.UserConsent;
import com.example.capshop.repository.UserConsentRepository;
import com.example.capshop.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserConsentRepository userConsentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCouponService userCouponService;

    public User save(User user){
        User savedUser = userRepository.save(user);
        // new user saved; welcome coupon issuance attempted
        issueWelcomeCouponAfterCommit(savedUser.getId(), "userId");
        return savedUser;
    }
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByNameAndPhone(String name, String phone) {
        String normalizedName = normalizeText(name);
        String normalizedPhone = normalizeText(phone);
        if (!StringUtils.hasText(normalizedName) || !StringUtils.hasText(normalizedPhone)) {
            return Optional.empty();
        }
        return userRepository.findByNameAndPhone(normalizedName, normalizedPhone);
    }

    public Optional<User> findByEmailAndPhone(String email, String phone) {
        String normalizedEmail = normalizeText(email);
        String normalizedPhone = normalizeText(phone);
        if (!StringUtils.hasText(normalizedEmail) || !StringUtils.hasText(normalizedPhone)) {
            return Optional.empty();
        }
        return userRepository.findByEmailAndPhone(normalizedEmail, normalizedPhone);
    }

    // 프로바이더 + providerUserId로 연결된 유저 조회 (소셜 연동 확인용)
    public User findByProviderAndProviderUserId(String provider, String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank()) {
            return null;
        }
        try {
            AuthProvider authProvider = AuthProvider.valueOf(provider.toUpperCase());
            return userRepository.findByOauthProviderAndProviderUserId(authProvider, providerUserId).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // 내부 업데이트 용도: 웰컴쿠폰 발급 없이 저장
    @Transactional
    public User saveWithoutWelcomeCoupon(User user) {
        return userRepository.save(user);
    }

    // 소셜 가입 완료 시 실제 User 생성 로직 (추가 필드 포함 가능)
    @Transactional
    public User createSocialUser(String email, String name, String provider, String providerUserId, String phone) {
        AuthProvider authProvider = AuthProvider.valueOf(provider.toUpperCase());

        // 이미 provider+id로 연결된 사용자가 있는지 확인
        if (userRepository.findByOauthProviderAndProviderUserId(authProvider, providerUserId).isPresent()) {
            throw new IllegalArgumentException("이미 해당 소셜 계정으로 가입된 사용자입니다.");
        }

        // 이메일이 이미 존재하면(로컬 계정 등) 분기 처리: 여기서는 에러로 처리하거나 병합하도록 요구
        if (email != null && userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("동일 이메일로 이미 가입된 계정이 존재합니다. 먼저 로그인 후 소셜 계정을 연결하세요.");
        }

        User newUser = User.builder()
                .email(email)
                .name(name)
                .phone(phone)
                .oauthProvider(authProvider)
                .providerUserId(providerUserId)
                .build();

        User savedUser = userRepository.save(newUser);

        // 웰컴 쿠폰 지급 (소셜 가입은 트랜잭션 안에서 수행되므로 커밋 이후에 발급)
        issueWelcomeCouponAfterCommit(savedUser.getId(), "social userId");

        return savedUser;
    }
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    // 관리자 권한 토글 (승격/해제)
    public boolean toggleAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setAdmin(!user.isAdmin());
        userRepository.save(user);
        return user.isAdmin();
    }

    // 사용자 상태 토글 (활성화/비활성화)
    public boolean toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setDeleted(!user.isDeleted());
        userRepository.save(user);
        return user.isDeleted();
    }

   
    @Transactional
    public User findOrCreateUser(String email, String name, String provider, String providerUserId) {
        AuthProvider authProvider = AuthProvider.valueOf(provider.toUpperCase());
        return userRepository.findByEmail(email)
                .map(user -> {
                    // existing social user found; updating info
                    // 이미 존재하는 유저 → 정보만 업데이트
                    user.setName(name);
                    user.setOauthProvider(authProvider);
                    user.setProviderUserId(providerUserId);
                    return user; // 영속 상태라 save() 안 해도 flush 시점에 반영됨
                })
                .orElseGet(() -> {
                    // ✅ 여기서 "새로 가입" → 웰컴 쿠폰 지급 대상으로 삼기
                    // creating new social user
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .oauthProvider(authProvider)
                            .providerUserId(providerUserId)
                            .build();

                    User savedUser = userRepository.save(newUser);

                    // ✅ 소셜 로그인 가입자도 웰컴 쿠폰 지급
                    issueWelcomeCouponAfterCommit(savedUser.getId(), "oauth userId");

                    return savedUser;
                });
    }

    // 로컬 회원가입 (전화번호 포함)
    public User createLocalUser(String email, String password, String name, String phone) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        if (phone != null && !phone.isBlank() && userRepository.existsByPhone(phone.trim())) {
            throw new IllegalArgumentException("이미 사용 중인 전화번호입니다.");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User newUser = User.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .phone(phone != null && !phone.isBlank() ? phone.trim() : null)
                .oauthProvider(AuthProvider.LOCAL)
                .providerUserId(null)
                .build();
        User savedUser = userRepository.save(newUser);
        // local user created; issue welcome coupon
        issueWelcomeCouponAfterCommit(savedUser.getId(), "local userId");
        return savedUser;
    }

    private void issueWelcomeCouponAfterCommit(Long userId, String label) {
        if (userId == null) return;

        Runnable issuer = () -> {
            try {
                userCouponService.issueWelcomeCouponToNewUser(userId);
            } catch (Exception e) {
                log.warn("welcome coupon issuance failed for {}={}: {}", label, userId, e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    issuer.run();
                }
            });
        } else {
            issuer.run();
        }
    }

    // 로컬 로그인 인증
    public Optional<User> authenticateLocalUser(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && user.get().getOauthProvider() == AuthProvider.LOCAL) {
            if (passwordEncoder.matches(password, user.get().getPassword())) {
                return user;
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void resetPassword(String email, String phone, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        }

        User user = findByEmailAndPhone(email, phone)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다."));

        if (user.getOauthProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 재설정할 수 없습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }
    
    // 주소 목록 조회
    public List<String> getAddresses(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }
        return user.getAddress() != null ? user.getAddress() : List.of();
    }
    
    // 주소 추가
    public void addAddress(Long userId, String address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (user.getAddress() == null) {
            user.setAddress(new java.util.ArrayList<>());
        }
        
        if (!user.getAddress().contains(address)) {
            user.getAddress().add(address);
            userRepository.save(user);
        }
    }
    
    // 주소 삭제
    public void removeAddress(Long userId, String address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (user.getAddress() != null) {
            user.getAddress().remove(address);
            userRepository.save(user);
        }
    }
    
    // 사용자 정보 수정
        @Transactional
        public void updateUserProfile(Long userId, String name, String phone, Boolean emailMarketing, Boolean smsMarketing,
            String ip, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }

        // phone 미전송(null/blank) 시 기존값 유지
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone.trim());
        }

        // 마케팅 동의는 UserConsent에 "최신 상태"를 기록 (히스토리 보존)
        saveMarketingConsentIfPresent(user, "MARKETING_EMAIL", emailMarketing, ip, userAgent);
        saveMarketingConsentIfPresent(user, "MARKETING_SMS", smsMarketing, ip, userAgent);

        userRepository.save(user);
    }

    private void saveMarketingConsentIfPresent(User user, String consentType, Boolean agreed, String ip, String userAgent) {
        if (agreed == null) {
            return;
        }

        UserConsent consent = new UserConsent();
        consent.setUser(user);
        consent.setConsentType(consentType);
        consent.setAgreed(Boolean.TRUE.equals(agreed));
        consent.setVersion("v2025.09");
        consent.setTimestamp(Instant.now());
        consent.setIp(ip);
        consent.setUserAgent(userAgent);
        userConsentRepository.save(consent);
    }
    
    // 계정 탈퇴 (소프트 삭제)
    public void deleteUserAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        user.setDeleted(true);
        userRepository.save(user);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
