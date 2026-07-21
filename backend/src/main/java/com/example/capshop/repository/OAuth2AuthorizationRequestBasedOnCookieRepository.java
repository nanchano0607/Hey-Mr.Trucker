package com.example.capshop.repository;

import com.example.capshop.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public class OAuth2AuthorizationRequestBasedOnCookieRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_EXPIRE_SECONDS = 18000; // 5h

    // 로컬 개발용 (http). 운영(HTTPS) 전환 시: SECURE=true, SAME_SITE="None"
    private static final boolean SECURE = false;
    private static final String  SAME_SITE = "Lax";

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = CookieUtil.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME).orElse(null);
        if (cookie == null) {
            return null;
        }
        return CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }
        String serialized = CookieUtil.serialize(authorizationRequest);
        CookieUtil.addCookie(response,
                OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                serialized,
                COOKIE_EXPIRE_SECONDS,
                SECURE,
                SAME_SITE);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        // 스프링 시큐리티의 계약상 보통 여기선 로드만 하고,
        // 실제 삭제는 성공/실패 핸들러에서 별도로 처리한다.
        return loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        // request는 사용하지 않지만, 시그니처는 유지
        CookieUtil.deleteCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, SECURE, SAME_SITE);
    }
}
