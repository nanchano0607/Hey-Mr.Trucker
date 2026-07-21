package com.example.capshop.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.config.OAuth2SuccessHandler;
import com.example.capshop.config.TokenProvider;
import com.example.capshop.domain.AuthProvider;
import com.example.capshop.domain.RefreshToken;
import com.example.capshop.domain.User;
import com.example.capshop.domain.UserConsent;
import com.example.capshop.repository.RefreshTokenRepository;
import com.example.capshop.repository.UserConsentRepository;
import com.example.capshop.service.SocialSignupTokenService;
import com.example.capshop.service.UserService;
import com.example.capshop.util.CookieUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final SocialSignupTokenService socialSignupTokenService;
    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserConsentRepository userConsentRepository;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/complete-signup")
    public ResponseEntity<?> completeSignup(
            @RequestBody Map<String, Object> req,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        try {
            String token = Objects.toString(req.get("token"), "").trim();
            String phone = Objects.toString(req.get("phone"), "").trim();

            Map<String, Boolean> agreements = coerceAgreements(req.get("agreements"));

            // 1) 기본 검증
            if (token.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "회원가입 토큰이 필요합니다."));
            }
            if (phone.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "전화번호를 입력해주세요."));
            }
            if (agreements.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "동의 항목이 필요합니다."));
            }

            // 2) 소셜 가입용 토큰 파싱
            Claims claims = socialSignupTokenService.parseToken(token);
            String provider = claims.get("provider", String.class);
            String providerUserId = claims.get("providerUserId", String.class);
            String email = claims.get("email", String.class);
            String name = claims.get("name", String.class);

            if (provider == null || provider.isBlank() || providerUserId == null || providerUserId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 회원가입 토큰입니다."));
            }

            // 3) 중복 검증 (트랜잭션 밖에서 수행)
            // 전화번호 중복 체크
            if (userService.existsByPhone(phone)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "이미 가입된 전화번호입니다."));
            }
            
            // provider+providerUserId 중복 체크
            if (userService.findByProviderAndProviderUserId(provider, providerUserId) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "이미 해당 소셜 계정으로 가입된 사용자입니다."));
            }

            // 이메일 중복 체크 (단, 과거에 "소셜 유저"를 email 기준으로 먼저 저장하면서 providerUserId가 비어있는 레코드는 보완 허용)
            User legacySocialStub = null;
            if (email != null) {
                var existingByEmail = userService.findByEmail(email);
                if (existingByEmail.isPresent()) {
                    User u = existingByEmail.get();
                    AuthProvider p;
                    try {
                        p = AuthProvider.valueOf(provider.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 소셜 로그인 제공자입니다."));
                    }

                    boolean missingProviderId = (u.getProviderUserId() == null || u.getProviderUserId().isBlank());
                    if (u.getOauthProvider() == p && missingProviderId) {
                        legacySocialStub = u;
                    } else {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "동일 이메일로 이미 가입된 계정이 존재합니다."));
                    }
                }
            }

            // 4) 유저 생성/보완
            User user;
            if (legacySocialStub != null) {
                legacySocialStub.setProviderUserId(providerUserId);
                legacySocialStub.setPhone(phone);
                if (name != null && !name.isBlank()) legacySocialStub.setName(name);
                user = userService.saveWithoutWelcomeCoupon(legacySocialStub);
            } else {
                user = userService.createSocialUser(email, name, provider, providerUserId, phone);
            }

            // 4.5) 동의 정보 저장 (IP, User-Agent 포함)
            String clientIp = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            saveConsents(user, agreements, clientIp, userAgent);

            // 5) RT 발급 + 저장
            String refreshToken = tokenProvider.generateToken(user, OAuth2SuccessHandler.REFRESH_TOKEN_DURATION);
            RefreshToken rt = refreshTokenRepository.findByUserId(user.getId())
                    .map(entity -> entity.update(refreshToken))
                    .orElse(new RefreshToken(user.getId(), refreshToken));
            @SuppressWarnings({"null", "unused"})
            RefreshToken ignored = refreshTokenRepository.save(rt);

            // 6) RT 쿠키 심기
            int maxAge = (int) OAuth2SuccessHandler.REFRESH_TOKEN_DURATION.toSeconds();
            CookieUtil.deleteCookie(response, OAuth2SuccessHandler.REFRESH_TOKEN_COOKIE_NAME, cookieSecure, cookieSameSite);
            CookieUtil.addCookie(response, OAuth2SuccessHandler.REFRESH_TOKEN_COOKIE_NAME, refreshToken, maxAge, cookieSecure, cookieSameSite);

            return ResponseEntity.ok(Map.of(
                    "message", "signup_complete",
                    "userId", user.getId()
            ));
        } catch (ExpiredJwtException e) {
            log.warn("complete-signup failed: token expired", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "회원가입 토큰이 만료되었습니다. 다시 시도해주세요."));
        } catch (JwtException e) {
            log.warn("complete-signup failed: invalid token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "유효하지 않은 회원가입 토큰입니다."));
        } catch (DataIntegrityViolationException e) {
            // 예: phone unique 제약 위반 등
            log.warn("complete-signup failed: data integrity violation", e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "이미 가입된 정보가 존재합니다."));
        } catch (IllegalArgumentException e) {
            // createSocialUser()에서 중복/검증 실패를 IllegalArgumentException으로 던짐
            log.warn("complete-signup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("complete-signup failed: unexpected error", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "요청 처리 중 오류가 발생했습니다."));
        }
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
            consent.setConsentType(entry.getKey());
            consent.setAgreed(Boolean.TRUE.equals(entry.getValue()));
            consent.setVersion("v2025.09");
            consent.setTimestamp(Instant.now());
            consent.setIp(ip);
            consent.setUserAgent(userAgent);
            userConsentRepository.save(consent);
        }
    }

    private Map<String, Boolean> coerceAgreements(Object agreementsObj) {
        if (!(agreementsObj instanceof Map<?, ?> raw)) {
            return Map.of();
        }

        Map<String, Boolean> agreements = new HashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() == null) continue;
            String key = entry.getKey().toString();
            Object v = entry.getValue();
            Boolean b;

            if (v == null) {
                b = false;
            } else if (v instanceof Boolean bool) {
                b = bool;
            } else if (v instanceof String s) {
                b = Boolean.parseBoolean(s);
            } else if (v instanceof Number n) {
                b = n.intValue() != 0;
            } else {
                // 알 수 없는 타입이면 저장하지 않음
                continue;
            }

            agreements.put(key, b);
        }

        return agreements;
    }
}
