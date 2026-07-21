package com.example.capshop.config;

import com.example.capshop.repository.OAuth2AuthorizationRequestBasedOnCookieRepository;
import com.example.capshop.repository.RefreshTokenRepository;
import com.example.capshop.service.OAuth2UserCustomService;
import com.example.capshop.service.UserService;
import lombok.RequiredArgsConstructor;
import com.example.capshop.service.SocialSignupTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.List;

@RequiredArgsConstructor
@Configuration
public class WebOAuthSecurityConfig {

    private final OAuth2UserCustomService oAuth2UserCustomService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final SocialSignupTokenService socialSignupTokenService;
    
    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins}")
    private String allowedOriginsProp;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.base-url:}")
    private String frontendBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @org.springframework.beans.factory.annotation.Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    // 카카오에서 scope 변경 후 기존 사용자에게 동의 화면을 다시 띄우고 싶을 때 사용
    // 예: consent (동의 재요청), login (재로그인 유도)
    @org.springframework.beans.factory.annotation.Value("${app.oauth2.kakao.prompt:}")
    private String kakaoPrompt;


    @Bean
    public WebSecurityCustomizer configure() {
        return web -> web.ignoring().requestMatchers("/img/**", "/css/**", "/js/**");
    }

    /** 체인 #1: /api/token 전용 (토큰 없이 허용, JWT 필터 없음) */
    @Bean
    @Order(1)
    public SecurityFilterChain tokenChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/token")
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .httpBasic(h -> h.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** 체인 #2: 나머지 (API는 인증 필요, 그 외는 허용 + OAuth2 + JWT 필터) */
   @Bean
@Order(2)
public SecurityFilterChain appChain(HttpSecurity http, HandlerMappingIntrospector introspector, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
    MvcRequestMatcher apiMatcher = new MvcRequestMatcher(introspector, "/api/**");

    http
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .httpBasic(h -> h.disable())
        .formLogin(f -> f.disable())
        .logout(l -> l.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // 관리자
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/stockist").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/stockist/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/toss/webhook").permitAll()

            // 인증 없이 허용 (공개 API)
            .requestMatchers(HttpMethod.GET,
                "/api/products/**",
                "/api/product/**",
                "/api/popup/**",
                "/api/cap/**",
                "/api/acc/**",
                "/api/vintage/**",
                "/api/notices/**",
                "/api/reviews/**",
                "/api/story",
                "/api/logo",
                "/api/background",
                "/api/logbook",
                "/api/stockist"
            ).permitAll()

            // 인증 없이 허용 (로그인/토큰/휴대폰)
            .requestMatchers(
                "/api/token",
                "/api/auth/**",
                "/api/phone/send",
                "/api/phone/verify",
                "/api/user/id/overlap" // 이메일(아이디) 중복 확인
            ).permitAll()

            // 그 외 API는 인증 필요
            .requestMatchers("/api/**").authenticated()

            // 페이지/정적은 허용 (SPA)
            .anyRequest().permitAll()
        )
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/login")
            .authorizationEndpoint(endpoint ->
                endpoint
                    .authorizationRequestRepository(oAuth2AuthorizationRequestBasedOnCookieRepository())
                    .authorizationRequestResolver(oAuth2AuthorizationRequestResolver(clientRegistrationRepository))
            )
            .successHandler(oAuth2SuccessHandler())
            .userInfoEndpoint(endpoint -> endpoint
                .userService(oAuth2UserCustomService)
                .oidcUserService(new OidcUserService())
            )
        )
        .exceptionHandling(exception -> exception
            .defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                apiMatcher
            )
        )
        .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

    private OAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver(ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return customize(defaultResolver.resolve(request));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                return customize(defaultResolver.resolve(request, clientRegistrationId));
            }

            private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest authRequest) {
                if (authRequest == null) return null;

                Object regIdObj = authRequest.getAttributes().get(OAuth2ParameterNames.REGISTRATION_ID);
                String registrationId = regIdObj != null ? regIdObj.toString() : null;

                if (!"kakao".equals(registrationId)) {
                    return authRequest;
                }

                if (kakaoPrompt == null || kakaoPrompt.isBlank()) {
                    return authRequest;
                }

                LinkedHashMap<String, Object> params = new LinkedHashMap<>(authRequest.getAdditionalParameters());
                params.put("prompt", kakaoPrompt.trim());
                return OAuth2AuthorizationRequest.from(authRequest)
                        .additionalParameters(params)
                        .build();
            }
        };
    }

    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return new OAuth2SuccessHandler(
            tokenProvider,
            refreshTokenRepository,
            oAuth2AuthorizationRequestBasedOnCookieRepository(),
            userService,
            socialSignupTokenService,
            frontendBaseUrl,
            cookieSecure,
            cookieSameSite
        );
        
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }

    @Bean
    public OAuth2AuthorizationRequestBasedOnCookieRepository oAuth2AuthorizationRequestBasedOnCookieRepository() {
        return new OAuth2AuthorizationRequestBasedOnCookieRepository();
    }

    /** CORS: 프론트(5173)에서 쿠키 전송 허용 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
    // Read configured origins from properties
    List<String> allowedOrigins = List.of(allowedOriginsProp.split("\\s*,\\s*"));

        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true);
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
