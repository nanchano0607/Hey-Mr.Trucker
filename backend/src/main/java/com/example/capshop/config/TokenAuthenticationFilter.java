package com.example.capshop.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    /** 이 조건에 해당하면 아예 필터를 타지 않게 함 */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();

        // 3) 프리플라이트는 스킵
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        // 1) API가 아니면 스킵
        if (path == null || !path.startsWith("/api/")) return true;

        // 2) 토큰 발급 엔드포인트는 스킵 (체인 #1에서 이미 permitAll)
        if ("/api/token".equals(path)) return true;

        

        // (선택) OAuth2 시작/콜백도 스킵하고 싶다면:
        // if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) return true;

        return false; // 위 조건에 안 걸리면 필터 실행
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER_AUTHORIZATION);

        // Authorization 헤더 없거나 형식 아님 → 조용히 패스
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(TOKEN_PREFIX.length()).trim();

        // 빈 문자열 / "null" / "undefined" 방지 → 조용히 패스
        if (token.isEmpty()
                || "null".equalsIgnoreCase(token)
                || "undefined".equalsIgnoreCase(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 유효하지 않으면 인증 세팅 없이 통과 (로그인 필요 엔드포인트는 결국 401을 주게 됨)
            if (!tokenProvider.validToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }
            Authentication authentication = tokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // 필요시 디버그 로그만
            // log.debug("JWT processing failed: {}", e.getMessage());
            // 인증은 세팅하지 않고 계속 진행
        }

        filterChain.doFilter(request, response);
    }
}
