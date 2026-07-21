package com.example.capshop.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.capshop.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialSignupTokenService {
    private final JwtProperties jwtProperties;

    // 짧은 만료 시간 (테스트/프로덕션 조정 가능)
    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(15);

    public String createToken(String provider, String providerUserId, String email, String name) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + DEFAULT_DURATION.toMillis());

        SecretKey secretKey = new SecretKeySpec(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());

        Map<String, Object> claims = new HashMap<>();
        claims.put("provider", provider);
        claims.put("providerUserId", providerUserId);
        claims.put("email", email);
        claims.put("name", name);

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .addClaims(claims)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        SecretKey secretKey = new SecretKeySpec(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
