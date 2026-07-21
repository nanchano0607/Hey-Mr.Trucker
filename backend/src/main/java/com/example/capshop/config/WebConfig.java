package com.example.capshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;
    private final String uploadDir;

    public WebConfig(
            @Value("${app.cors.allowed-origins}") String allowedOrigins,
            @Value("${app.upload-dir:${user.home}/capshop/uploads}") String uploadDir
    ) {
        this.allowedOrigins = Arrays.asList(allowedOrigins.split("\s*,\s*"));
        this.uploadDir = uploadDir;
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Global mapping for frontends
        registry.addMapping("/**")
            .allowedOrigins(allowedOrigins.toArray(new String[0]))
            .allowedMethods("*")
            .allowCredentials(true);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        PathPatternParser parser = new PathPatternParser();
        parser.setMatchOptionalTrailingSeparator(true);
        configurer.setPatternParser(parser);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = uploadDir;
        if (!base.endsWith("/")) base = base + "/";

        // 업로드 파일(상품/리뷰 등): /uploads/** -> {app.upload-dir}/**
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + base);

        // 레거시/고정 이미지: /images/** -> {app.upload-dir}/cap/**
        // (프론트에서 /images/emptyload.png 같은 경로를 사용)
        registry.addResourceHandler("/images/**")
            .addResourceLocations("file:" + base + "cap/");
    }

}
