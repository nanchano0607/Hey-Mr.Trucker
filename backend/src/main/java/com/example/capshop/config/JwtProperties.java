package com.example.capshop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
public class JwtProperties {
    @Value("${jwt.secret}")
    private String secretKey;
}
