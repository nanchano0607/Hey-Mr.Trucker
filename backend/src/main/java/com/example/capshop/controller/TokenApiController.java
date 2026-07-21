package com.example.capshop.controller;

import com.example.capshop.config.OAuth2SuccessHandler;
import com.example.capshop.domain.User;
import com.example.capshop.dto.CreateAccessTokenResponse;
import com.example.capshop.dto.UserProfile;
import com.example.capshop.service.TokenService;
import com.example.capshop.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ✅ 추가

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j // ✅ 추가
public class TokenApiController {

    private final TokenService tokenService;
    private static final String RT_COOKIE = OAuth2SuccessHandler.REFRESH_TOKEN_COOKIE_NAME;


    @PostMapping("/token")
    public ResponseEntity<CreateAccessTokenResponse> createAccessToken(HttpServletRequest request) {
        // 1) 쿠키에서 리프레시 토큰 읽기 (없으면 401)
        String refreshToken = CookieUtil.getCookieValue(request, RT_COOKIE)
                .orElseThrow(() -> {
                    //log.warn("[/api/token] No refresh token cookie ({}) found → 401", RT_COOKIE);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token");
                });

//        log.info("[/api/token] refreshToken(cookie={}): {}", RT_COOKIE, mask(refreshToken));

        // 2) JWT 유효성만 확인해서 새 Access Token 발급
        String newAccessToken = tokenService.createdNewAccessToken(refreshToken);

        // 3) OK
        CreateAccessTokenResponse body = new CreateAccessTokenResponse(newAccessToken);
       // log.info("[/api/token] issuing new access token: {}", mask(newAccessToken));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
      //  log.debug("[/api/me] authentication = {}", (authentication == null ? "null" : authSummary(authentication)));

        // 비로그인 → 401
        if (authentication == null ||
            !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken) {
            Map<String, String> unauth = Map.of("message", "unauthenticated");
          //  log.info("[/api/me] unauthenticated → 401, body={}", unauth);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(unauth);
        }

        Object principal = authentication.getPrincipal();
     //   log.debug("[/api/me] principal class = {}", principal.getClass().getName());

        // 1) OAuth2 로그인 (Google/Kakao/Naver)
        if (principal instanceof OAuth2User ou) {
            Map<String, Object> attrs = new HashMap<>();
            String email = resolveEmail(ou);
            String name  = resolveName(ou, authentication.getName());
            attrs.put("id", null); // OAuth2 로그인은 id를 알 수 없으므로 null
            attrs.put("email", email);
            attrs.put("name", name);

            UserProfile profile = new UserProfile(attrs);
         //   log.info("[/api/me] OAuth2User resolved → email={}, name={}", email, name);
          //  log.debug("[/api/me] response body={}", profile);
            return ResponseEntity.ok(profile);
        }

        // 2) 폼 로그인/기본 UserDetails
        if (principal instanceof User user) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("id", user.getId());
            attrs.put("email", user.getEmail());
            attrs.put("name", user.getName());
            attrs.put("isAdmin", user.isAdmin()); // ← 추가

            UserProfile profile = new UserProfile(attrs);
       //     log.info("[/api/me] User resolved → id={}, email={}, name={}", user.getId(), user.getEmail(), user.getName());
        //    log.debug("[/api/me] response body={}", profile);
            return ResponseEntity.ok(profile);
        } else if (principal instanceof UserDetails ud) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("id", null); // id를 알 수 없음
            attrs.put("email", ud.getUsername());
            attrs.put("name", ud.getUsername());

            UserProfile profile = new UserProfile(attrs);
      //      log.info("[/api/me] UserDetails resolved → username={}", ud.getUsername());
        //    log.debug("[/api/me] response body={}", profile);
            return ResponseEntity.ok(profile);
        }

        // 3) 그 외 타입
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", null);
        attrs.put("email", null);
        attrs.put("name", authentication.getName());

        UserProfile profile = new UserProfile(attrs);
    //    log.info("[/api/me] Other principal → name={}", authentication.getName());
     //   log.debug("[/api/me] response body={}", profile);
        return ResponseEntity.ok(profile);
    }

    @SuppressWarnings("unchecked")
    private String resolveEmail(OAuth2User u) {
        Object direct = u.getAttribute("email");
        if (direct instanceof String s && !s.isBlank()) return s;

        Map<String, Object> kakaoAccount = u.getAttribute("kakao_account");
        if (kakaoAccount != null) {
            Object e = kakaoAccount.get("email");
            if (e instanceof String s && !s.isBlank()) return s;
        }

        Map<String, Object> response = u.getAttribute("response");
        if (response != null) {
            Object e = response.get("email");
            if (e instanceof String s && !s.isBlank()) return s;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private String resolveName(OAuth2User u, String fallback) {
        Object n = u.getAttribute("name");
        if (n instanceof String s && !s.isBlank()) return s;

        Map<String, Object> kakaoAccount = u.getAttribute("kakao_account");
        if (kakaoAccount != null) {
            Object profileObj = kakaoAccount.get("profile");
            if (profileObj instanceof Map<?, ?> m) {
                Object nick = ((Map<String, Object>) m).get("nickname");
                if (nick instanceof String s && !s.isBlank()) return s;
            }
        }

        Map<String, Object> response = u.getAttribute("response");
        if (response != null) {
            Object name = response.get("name");
            if (name instanceof String s && !s.isBlank()) return s;
            Object nn = response.get("nickname");
            if (nn instanceof String s && !s.isBlank()) return s;
        }

        Object given = u.getAttribute("given_name");
        if (given instanceof String s && !s.isBlank()) return s;

        return fallback;
    }

    // ===== helpers =====
    private static String mask(String token) {
        if (token == null) return "null";
        int len = token.length();
        if (len <= 10) return "****(len=" + len + ")";
        return token.substring(0, 6) + "..." + token.substring(len - 4) + "(len=" + len + ")";
    }

    private static String authSummary(Authentication a) {
        return String.format("authenticated=%s, name=%s, principalClass=%s",
                a.isAuthenticated(), a.getName(),
                a.getPrincipal() == null ? "null" : a.getPrincipal().getClass().getName());
    }
}
