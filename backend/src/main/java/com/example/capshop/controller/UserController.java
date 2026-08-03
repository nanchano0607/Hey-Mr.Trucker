package com.example.capshop.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.AuthProvider;
import com.example.capshop.config.TokenProvider;
import com.example.capshop.domain.RefreshToken;
import com.example.capshop.domain.User;
import com.example.capshop.domain.UserConsent;
import com.example.capshop.dto.FindUserIdRequest;
import com.example.capshop.dto.LoginRequest;
import com.example.capshop.dto.ResetPasswordRequest;
import com.example.capshop.dto.UserAdminResponse;
import com.example.capshop.dto.SignupRequest;
import com.example.capshop.repository.RefreshTokenRepository;
import com.example.capshop.repository.UserConsentRepository;
import com.example.capshop.service.UserService;
import com.example.capshop.service.PhoneVerificationService;
import com.example.capshop.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class UserController {
    private final UserService userService;
    private final UserConsentRepository userConsentRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PhoneVerificationService phoneVerificationService;
    
    // 로그아웃: RT 삭제 + 쿠키 제거
    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response,
                                    @AuthenticationPrincipal User user) {
        try {
            // logout requested
            if (user != null) {
                // logout user info available
                refreshTokenRepository.findByUserId(user.getId()).ifPresent(rt -> {
                    refreshTokenRepository.delete(rt);
                    // deleted refresh token for user
                });
            } else {
                // anonymous logout request - checking cookie-based RT
                CookieUtil.getCookieValue(request, "refresh_token").ifPresent(rt -> {
                    // found refresh_token cookie value
                    refreshTokenRepository.findByRefreshToken(rt).ifPresentOrElse(entity -> {
                        refreshTokenRepository.delete(entity);
                        // deleted refresh token by token value
                    }, () -> {
                        // no refresh token entity found for provided token
                    });
                });
            }

            // 쿠키 삭제 (로컬: secure=false, sameSite=Lax)
            CookieUtil.deleteCookie(response, "refresh_token", false, "Lax");
            CookieUtil.deleteCookie(response, "access_token", false, "Lax");
            // cleared cookies: refresh_token, access_token

            // SecurityContext 클리어
            SecurityContextHolder.clearContext();
            // security context cleared

            return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "로그아웃 중 오류가 발생했습니다."));
        }
    }
    
    @PostMapping("/api/user/save")
    public void saveUser(@RequestBody User user){
        userService.save(user);
    }
    // 관리자: 사용자 목록 (DTO로 반환하여 순환 참조/중첩 방지)
    @GetMapping("/api/admin/users")
    public List<UserAdminResponse> allUser(){
        return userService.findAll().stream()
                .map(UserAdminResponse::new)
                .toList();
    }

    // 관리자: 사용자 ID 목록 (프론트 배치 로딩용)
    @GetMapping("/api/admin/users/ids")
    public List<Long> allUserIds() {
        return userService.findAll().stream()
                .map(User::getId)
                .toList();
    }

    // 관리자: 개별 사용자 조회 (DTO)
    @GetMapping("/api/admin/users/{id}")
    public ResponseEntity<UserAdminResponse> getUser(@PathVariable("id") Long id) {
        User u = userService.findById(id);
        if (u == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new UserAdminResponse(u));
    }

    // 관리자: 권한 토글 (승격/해제)
    @PostMapping("/api/admin/users/{id}/toggle-admin")
    public ResponseEntity<Map<String, Object>> toggleAdmin(@PathVariable("id") Long id) {
        try {
            boolean isAdmin = userService.toggleAdmin(id);
            String message = isAdmin ? "관리자 권한이 부여되었습니다." : "관리자 권한이 해제되었습니다.";
            return ResponseEntity.ok(Map.of(
                "message", message,
                "admin", isAdmin
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 관리자: 사용자 상태 토글 (활성화/비활성화)
    @PostMapping("/api/admin/users/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable("id") Long id) {
        try {
            boolean isDeleted = userService.toggleUserStatus(id);
            String message = isDeleted ? "사용자가 비활성화되었습니다." : "사용자가 활성화되었습니다.";
            return ResponseEntity.ok(Map.of(
                "message", message,
                "deleted", isDeleted
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

 

    // 개별 사용자 정보 조회 (본인용)
    @GetMapping("/api/user/{id}")
    public ResponseEntity<UserAdminResponse> getUserProfile(@PathVariable("id") Long id) {
        User u = userService.findById(id);
        if (u == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new UserAdminResponse(u));
    }

    // 사용자 정보 수정 (본인용)
    @PostMapping("/api/user/{id}/update")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            String name = (String) request.get("name");
            String phone = (String) request.get("phone");
            Boolean emailMarketing = (Boolean) request.get("emailMarketing");
            Boolean smsMarketing = (Boolean) request.get("smsMarketing");

            // 동의 변경 시점의 부가정보 저장
            // NOTE: 프론트에서 phone을 제외해도 null이면 기존값 유지하도록 서비스에서 처리함
            String ip = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            userService.updateUserProfile(id, name, phone, emailMarketing, smsMarketing, ip, userAgent);
            
            return ResponseEntity.ok(Map.of("message", "정보가 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 계정 탈퇴 (본인용)
    @PostMapping("/api/user/{id}/delete")
    public ResponseEntity<Map<String, String>> deleteUserAccount(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User authenticatedUser,
            HttpServletResponse response) {
        try {
            if (authenticatedUser == null || !authenticatedUser.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "본인 계정만 탈퇴할 수 있습니다."));
            }

            userService.deleteUserAccount(id);
            refreshTokenRepository.findByUserId(id).ifPresent(refreshTokenRepository::delete);
            CookieUtil.deleteCookie(response, "refresh_token", false, "Lax");
            CookieUtil.deleteCookie(response, "access_token", false, "Lax");
            SecurityContextHolder.clearContext();

            return ResponseEntity.ok(Map.of("message", "계정이 탈퇴되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/api/auth/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        try {
            // 1. IP 주소와 User-Agent 추출
            String clientIp = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
                // Received signup request (sensitive fields are not logged)
            // 1.5 전화번호 필수 및 인증 확인
            String phone = request.getPhone();
            if (phone == null || phone.isBlank()) {
                return ResponseEntity.badRequest().body("전화번호를 입력하고 인증해주세요.");
            }

            boolean phoneVerified = phoneVerificationService.isVerified(phone);
            if (!phoneVerified) {
                return ResponseEntity.badRequest().body("전화번호 인증이 필요합니다.");
            }

            // 전화번호가 이미 등록된 계정이 있는지 확인
            if (userService.existsByPhone(phone)) {
                return ResponseEntity.badRequest().body("이미 사용 중인 전화번호입니다.");
            }

            // 2. 사용자 생성
            User user = userService.createLocalUser(request.getEmail(), request.getPassword(), request.getName(), phone);
            
            // 3. 동의 정보 저장 (IP, User-Agent 포함)
            saveConsents(user, request.getAgreements(), clientIp, userAgent);
            
            return ResponseEntity.ok("회원가입 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        Optional<User> userOpt = userService.authenticateLocalUser(request.getEmail(), request.getPassword());
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.isDeleted()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "code", "WITHDRAWN_USER",
                    "message", "탈퇴한 계정은 다시 로그인하거나 가입할 수 없습니다."
                ));
            }
            
            // 1) RT 발급 + DB 저장
            String refreshToken = tokenProvider.generateToken(user, Duration.ofDays(14));
            saveRefreshToken(user.getId(), refreshToken);
            
            // 2) RT를 HttpOnly 쿠키로 심기
            int maxAge = (int) Duration.ofDays(14).toSeconds();
            CookieUtil.addCookie(response, "refresh_token", refreshToken, maxAge, false, "Lax");
            
            // 3) Access Token 발급 + 응답에 포함
            String accessToken = tokenProvider.generateToken(user, Duration.ofHours(2));
            
            // 응답 데이터 생성
            Map<String, Object> responseData = Map.of(
                "message", "로그인 성공",
                "accessToken", accessToken,
                "user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "isAdmin", user.isAdmin()
                )
            );
            
            // responseData prepared
            
            return ResponseEntity.ok(responseData);
        } else {
            return ResponseEntity.badRequest().body("이메일 또는 비밀번호가 잘못되었습니다.");
        }
    }

    @PostMapping("/api/auth/find-id")
    public ResponseEntity<?> findUserId(@RequestBody FindUserIdRequest request) {
        String name = request.getName();
        String phone = request.getPhone();

        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이름과 전화번호를 입력해주세요."));
        }

        if (!phoneVerificationService.isVerified(phone.trim())) {
            return ResponseEntity.badRequest().body(Map.of("error", "전화번호 인증이 필요합니다."));
        }

        Optional<User> userOpt = userService.findByNameAndPhone(name, phone);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "일치하는 회원 정보를 찾을 수 없습니다."));
        }

        User user = userOpt.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "아이디를 찾을 수 없는 계정입니다."));
        }

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "maskedEmail", maskEmail(user.getEmail()),
                "authProvider", getAuthProviderName(user)
        ));
    }

    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        String email = request.getEmail();
        String phone = request.getPhone();
        String newPassword = request.getNewPassword();

        if (email == null || email.isBlank() || phone == null || phone.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일, 전화번호, 새 비밀번호를 모두 입력해주세요."));
        }

        if (!phoneVerificationService.isVerified(phone.trim())) {
            return ResponseEntity.badRequest().body(Map.of("error", "전화번호 인증이 필요합니다."));
        }

        try {
            userService.resetPassword(email, phone, newPassword);
            return ResponseEntity.ok(Map.of("message", "비밀번호가 재설정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    private void saveRefreshToken(Long userId, String newRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
                .map(entity -> entity.update(newRefreshToken))
                .orElse(new RefreshToken(userId, newRefreshToken));
        
        refreshTokenRepository.save(refreshToken);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private void saveConsents(User user, Map<String, Boolean> agreements, String ip, String userAgent) {
        for (Map.Entry<String, Boolean> entry : agreements.entrySet()) {
            UserConsent consent = new UserConsent();
            consent.setUser(user);
            consent.setConsentType(entry.getKey()); // "TERMS", "PRIVACY" 등
            consent.setAgreed(entry.getValue());
            consent.setVersion("v2025.09");
            consent.setTimestamp(Instant.now());
            consent.setIp(ip);
            consent.setUserAgent(userAgent);
            
            userConsentRepository.save(consent);
        }
    }
    
    // 주소 목록 조회
    @GetMapping("/api/user/{userId}/addresses")
    public ResponseEntity<List<String>> getAddresses(@PathVariable("userId") Long userId) {
        List<String> addresses = userService.getAddresses(userId);
        return ResponseEntity.ok(addresses);
    }
    
    // 주소 추가
    @PostMapping("/api/user/{userId}/addresses")
    public ResponseEntity<Map<String, String>> addAddress(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, String> request) {
        String address = request.get("address");
        if (address == null || address.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "주소를 입력해주세요."));
        }
        userService.addAddress(userId, address);
        return ResponseEntity.ok(Map.of("message", "주소가 추가되었습니다."));
    }
    
    // 주소 삭제
    @PostMapping("/api/user/{userId}/addresses/remove")
    public ResponseEntity<Map<String, String>> removeAddress(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, String> request) {
        String address = request.get("address");
        if (address == null || address.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "주소를 입력해주세요."));
        }
        userService.removeAddress(userId, address);
        return ResponseEntity.ok(Map.of("message", "주소가 삭제되었습니다."));
    }

    // 이메일(아이디) 중복 확인
    @PostMapping("/api/user/id/overlap")
    public ResponseEntity<Boolean> checkEmailOverlap(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(false);
        }
        boolean exists = userService.findByEmail(email).isPresent();
        return ResponseEntity.ok(exists);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }

        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domain;
        }

        return localPart.substring(0, 2) + "*".repeat(localPart.length() - 2) + domain;
    }

    private String getAuthProviderName(User user) {
        if (user.getOauthProvider() == null) {
            return AuthProvider.LOCAL.name();
        }
        return user.getOauthProvider().name();
    }
}
