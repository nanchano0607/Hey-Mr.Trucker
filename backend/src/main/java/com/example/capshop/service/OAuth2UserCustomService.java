package com.example.capshop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class OAuth2UserCustomService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final RestTemplate rest = new RestTemplate();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        String registrationId = req.getClientRegistration().getRegistrationId(); // "naver" | "kakao" | "google"
        log.info("[OAuth2UserCustomService] registrationId={}", registrationId);

        Map<String, Object> normalized;
        if ("naver".equals(registrationId)) {
            normalized = loadNaverUser(req);            // ← 네이버는 직접 호출 + 평탄화
        } else if ("kakao".equals(registrationId)) {
            OAuth2User raw = delegate.loadUser(req);    // ← 위임으로 받아온 뒤
            normalized = mapKakao(raw.getAttributes()); //    평탄화
        } else if ("google".equals(registrationId)) {
            OAuth2User raw = delegate.loadUser(req);
            normalized = mapGoogle(raw.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        // 권한 및 OAuth2User 생성(식별 키를 'provider_user_id'로 통일)
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        OAuth2User user = new DefaultOAuth2User(authorities, normalized, "provider_user_id");
        String provider = (String) normalized.get("provider");
        log.info("[OAuth2UserCustomService] normalized.provider={}", provider);

        return user;
    }

    /* -------------------- Provider별 매핑 -------------------- */

    // 네이버: userinfo 수동 호출 + 'response' 내부 평탄화
    private Map<String, Object> loadNaverUser(OAuth2UserRequest req) {
        String userInfoUri = req.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUri(); // https://openapi.naver.com/v1/nid/me

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(req.getAccessToken().getTokenValue());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> resp = rest.exchange(userInfoUri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> body = resp.getBody();
        if (body == null) throw new OAuth2AuthenticationException("Empty Naver response");

        Map<String, Object> r = (Map<String, Object>) body.get("response");
        if (r == null) throw new OAuth2AuthenticationException("Missing 'response' in Naver body");

        String id      = (String) r.get("id");
        String email   = (String) r.get("email");        // 동의 안 하면 null
        String name    = (String) r.get("name");
        String nick    = (String) r.get("nickname");
        String picture = (String) r.get("profile_image");
        String mobile  = (String) r.get("mobile");

        Map<String, Object> m = new HashMap<>();
        m.put("provider", "naver");
        m.put("provider_user_id", id);
        m.put("email", email);
        m.put("name", name != null ? name : nick);
        m.put("picture", picture);
        m.put("mobile", mobile);
        return m;
    }

    // 카카오: { id, kakao_account{ email, profile{nickname, profile_image_url} } }
    private Map<String, Object> mapKakao(Map<String, Object> attrs) {
        String id = String.valueOf(attrs.get("id"));
        Map<String, Object> account = (Map<String, Object>) attrs.get("kakao_account");
        Map<String, Object> profile = account != null ? (Map<String, Object>) account.get("profile") : null;

        String email   = account != null ? (String) account.get("email") : null;
        String name    = profile != null ? (String) profile.get("nickname") : null;
        String picture = profile != null ? (String) profile.get("profile_image_url") : null;

        if (account != null) {
            Object hasEmail = account.get("has_email");
            Object needsAgreement = account.get("email_needs_agreement");
            Object isVerified = account.get("is_email_verified");
            Object isValid = account.get("is_email_valid");
            log.info("[mapKakao] has_email={}, email_needs_agreement={}, is_email_verified={}, is_email_valid={}, emailPresent={}",
                    hasEmail, needsAgreement, isVerified, isValid, (email != null && !email.isBlank()));
        } else {
            log.info("[mapKakao] kakao_account is null; topLevelKeys={}", attrs.keySet());
        }

        Map<String, Object> m = new HashMap<>();
        m.put("provider", "kakao");
        m.put("provider_user_id", id);
        m.put("email", email);
        m.put("name", name);
        m.put("picture", picture);
        return m;
    }

    // 구글(OpenID): {sub, email, name, picture, ...}
    private Map<String, Object> mapGoogle(Map<String, Object> attrs) {
        String sub     = (String) attrs.get("sub");
        String email   = (String) attrs.get("email");
        String name    = (String) attrs.get("name");
        String picture = (String) attrs.get("picture");

        Map<String, Object> m = new HashMap<>();
        m.put("provider", "google");
        m.put("provider_user_id", sub);
        m.put("email", email);
        m.put("name", name);
        m.put("picture", picture);
        log.info("[mapGoogle] provider: {}", m.get("provider"));
        return m;
    }

}
