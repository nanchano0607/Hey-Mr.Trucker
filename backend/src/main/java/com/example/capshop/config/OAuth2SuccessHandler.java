package com.example.capshop.config;

import com.example.capshop.domain.RefreshToken;
import com.example.capshop.domain.User;
import com.example.capshop.repository.OAuth2AuthorizationRequestBasedOnCookieRepository;
import com.example.capshop.repository.RefreshTokenRepository;
import com.example.capshop.service.UserService;
import com.example.capshop.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(14);

    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuth2AuthorizationRequestBasedOnCookieRepository authorizationRequestRepository;
    private final UserService userService;
    private final com.example.capshop.service.SocialSignupTokenService socialSignupTokenService;
    private final String frontendBaseUrl;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public OAuth2SuccessHandler(
            TokenProvider tokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            OAuth2AuthorizationRequestBasedOnCookieRepository authorizationRequestRepository,
            UserService userService,
            com.example.capshop.service.SocialSignupTokenService socialSignupTokenService,
            String frontendBaseUrl,
            boolean cookieSecure,
            String cookieSameSite
    ) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.userService = userService;
        this.socialSignupTokenService = socialSignupTokenService;
        this.frontendBaseUrl = normalizeBase(frontendBaseUrl);
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = (cookieSameSite == null || cookieSameSite.isBlank()) ? "Lax" : cookieSameSite;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // 1) 공급자 구분: google / kakao / naver (절대 null 아님)
        String provider = null;
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            provider = oauth2Token.getAuthorizedClientRegistrationId();
        }
        log.info("provider={}", provider);

        // 2) 소셜 프로필 속성 꺼내기 (구글은 OIDC, 그 외 OAuth2)
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attrs = principal.getAttributes();

        // 구글 OIDC면 email/name 보장이 좋은 편
        if (principal instanceof OidcUser oidc) {
            attrs = oidc.getAttributes();
        }

        // 3) email / name / providerUserId 정규화 (카카오/네이버 대비)
        String email = extractEmail(provider, attrs);
        String name  = extractName(provider, attrs);
        String providerUserId = extractProviderId(provider, attrs);

        if (providerUserId == null || providerUserId.isBlank()) {
            log.warn("소셜 providerUserId 추출 실패: provider={}, attrKeys={}", provider, attrs.keySet());
            throw new IllegalArgumentException("소셜 계정 식별자(providerUserId)를 가져오지 못했습니다.");
        }

        // 이메일이 아예 없을 수 있음(카카오 미동의 등) → 임시 이메일 생성
        if (email == null || email.isBlank()) {
            //email = provider + "_" + providerUserId + "@placeholder.local";
            log.info("소셜 로그인 이메일 누락: provider={}, providerUserId={}", provider, providerUserId);

            clearAuthenticationAttributes(request, response);

            String msg = URLEncoder.encode("이메일 제공에 동의하지 않은 소셜 계정은 가입할 수 없습니다.", StandardCharsets.UTF_8);
            String redirectUrl = buildFrontendUrl("/login") + "?error=" + msg;
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        // 4) 이 소셜 계정이 기존 유저에 연결되어 있는지 확인
        User existingUser = userService.findByProviderAndProviderUserId(provider, providerUserId);

        if (existingUser != null) {
            if (existingUser.isDeleted()) {
                log.info("탈퇴한 소셜 계정 로그인 차단: userId={}, provider={}", existingUser.getId(), provider);
                clearAuthenticationAttributes(request, response);

                String msg = URLEncoder.encode(
                        "탈퇴한 계정은 다시 로그인하거나 가입할 수 없습니다.",
                        StandardCharsets.UTF_8
                );
                String redirectUrl = buildFrontendUrl("/login") + "?error=" + msg;
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                return;
            }

            log.info("소셜 계정이 기존 사용자에 연결됨: userId={}, email={}", existingUser.getId(), existingUser.getEmail());
            // 기존 유저: 기존 방식대로 토큰 발급 및 로그인 처리
            String refreshToken = tokenProvider.generateToken(existingUser, REFRESH_TOKEN_DURATION);
            saveRefreshToken(existingUser.getId(), refreshToken);

            int maxAge = (int) REFRESH_TOKEN_DURATION.toSeconds();
            CookieUtil.deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME, cookieSecure, cookieSameSite);
            CookieUtil.addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, maxAge, cookieSecure, cookieSameSite);

            clearAuthenticationAttributes(request, response);

            getRedirectStrategy().sendRedirect(request, response, buildFrontendUrl("/login/success"));
            return;
        }

        // 신규 소셜 계정: 아직 User를 만들지 않고, 추가 정보 입력 페이지로 리다이렉트
        log.info("신규 소셜 계정: provider={}, providerUserId={}, email={}, name={}", provider, providerUserId, email, name);
        String tempToken = socialSignupTokenService.createToken(provider, providerUserId, email, name);
        // 토큰은 민감하므로 전체를 로그에 남기지 않고 일부만 마스킹
        if (tempToken != null) {
            String masked = tempToken.length() <= 10 ? "(len=" + tempToken.length() + ")" : tempToken.substring(0, 6) + "..." + tempToken.substring(tempToken.length() - 4);
            log.info("생성된 소셜 임시 토큰: {}", masked);
        }

        clearAuthenticationAttributes(request, response);

        String tokenParam = URLEncoder.encode(tempToken, StandardCharsets.UTF_8);
        String redirectUrl = buildFrontendUrl("/complete-signup") + "?token=" + tokenParam;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private static String normalizeBase(String base) {
        if (base == null) return "";
        String b = base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b;
    }

    private String buildFrontendUrl(String path) {
        String p = (path == null) ? "" : path.trim();
        if (!p.startsWith("/")) p = "/" + p;
        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            // same-origin (백엔드 호스트 기준으로 프론트 라우팅)
            return p;
        }
        return frontendBaseUrl + p;
    }

    private void saveRefreshToken(Long userId, String newRefreshToken) {
        RefreshToken rt = refreshTokenRepository.findByUserId(userId)
                .map(entity -> entity.update(newRefreshToken))
                .orElse(new RefreshToken(userId, newRefreshToken));
        refreshTokenRepository.save(rt);
    }

    private void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    /* ========= 공급자별 attribute 정규화 유틸 ========= */

    private String extractEmail(String provider, Map<String, Object> attrs) {
        // OAuth2UserCustomService에서 평탄화된 형태로 넘어올 수 있음
        Object direct = attrs.get("email");
        if (direct instanceof String s && !s.isBlank()) return s;

        if ("google".equals(provider)) {
            return (String) attrs.get("email");
        } else if ("kakao".equals(provider)) {
            Map<String, Object> account = getMap(attrs, "kakao_account");
            return account != null ? (String) account.get("email") : null;
        } else if ("naver".equals(provider)) {
            // 네이버는 보통 {response:{email, name, ...}} 형태인데
            // 너의 OAuth2UserCustomService에서 평탄화해서 넘어왔다면 그냥 email 키에 있을 수도 있음.
            Object email = attrs.get("email");
            if (email instanceof String s) return s;
            Map<String, Object> resp = getMap(attrs, "response");
            return resp != null ? (String) resp.get("email") : null;
        }
        return null;
    }

    private String extractName(String provider, Map<String, Object> attrs) {
        // OAuth2UserCustomService에서 평탄화된 형태로 넘어올 수 있음
        Object direct = attrs.get("name");
        if (direct instanceof String s && !s.isBlank()) return s;

        if ("google".equals(provider)) {
            return (String) attrs.getOrDefault("name", (String) attrs.get("given_name"));
        } else if ("kakao".equals(provider)) {
            Map<String, Object> account = getMap(attrs, "kakao_account");
            Map<String, Object> profile = account != null ? getMap(account, "profile") : null;
            return profile != null ? (String) profile.get("nickname") : null;
        } else if ("naver".equals(provider)) {
            Object name = attrs.get("name");
            if (name instanceof String s) return s;
            Map<String, Object> resp = getMap(attrs, "response");
            return resp != null ? (String) resp.get("name") : null;
        }
        return null;
    }

    private String extractProviderId(String provider, Map<String, Object> attrs) {
        // OAuth2UserCustomService에서 식별키를 provider_user_id로 통일해서 넘김
        Object pid = attrs.get("provider_user_id");
        if (pid != null) return String.valueOf(pid);

        if ("google".equals(provider)) {
            return Optional.ofNullable((String) attrs.get("sub")).orElse((String) attrs.get("id"));
        } else if ("kakao".equals(provider)) {
            // kakao id는 Long이어서 String 변환 필요
            Object id = attrs.get("id");
            return id != null ? String.valueOf(id) : null;
        } else if ("naver".equals(provider)) {
            Object id = attrs.get("id");
            if (id instanceof String s) return s;
            Map<String, Object> resp = getMap(attrs, "response");
            Object rid = resp != null ? resp.get("id") : null;
            return rid != null ? rid.toString() : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> parent, String key) {
        Object v = parent.get(key);
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return null;
    }
}
