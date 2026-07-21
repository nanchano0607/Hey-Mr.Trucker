package com.example.capshop.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import org.springframework.util.SerializationUtils;

public class CookieUtil {

    /** 쿠키 조회 */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .findFirst();
    }

    /** 값만 조회 */
    public static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        return getCookie(request, name).map(Cookie::getValue);
    }

    /**
     * HttpOnly 쿠키 심기 (SameSite/Secure까지)
     * - 로컬(http): secure=false, sameSite="Lax"
     * - 운영(https): secure=true,  sameSite="None"
     */
    public static void addCookie(HttpServletResponse response, String name, String value,
                                         int maxAgeSeconds, boolean secure, String sameSite) {
        // javax Cookie API는 SameSite 지원이 약하므로 헤더로 직접 세팅
        String header = String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly%s; SameSite=%s",
                name, value, maxAgeSeconds,
                secure ? "; Secure" : "",
                sameSite
        );
        response.addHeader("Set-Cookie", header);
    }

    /** 쿠키 삭제 (심을 때와 동일 속성으로) */
    public static void deleteCookie(HttpServletResponse response, String name,
                                    boolean secure, String sameSite) {
        String header = String.format("%s=; Max-Age=0; Path=/; HttpOnly%s; SameSite=%s",
                name,
                secure ? "; Secure" : "",
                sameSite
        );
        response.addHeader("Set-Cookie", header);
    }
    public static String serialize(Object obj) {
        byte[] bytes = SerializationUtils.serialize(obj);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    /** 쿠키 값(Base64) → 객체 역직렬화 */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
        Object obj = SerializationUtils.deserialize(bytes);
        return cls.cast(obj);
    }
}
