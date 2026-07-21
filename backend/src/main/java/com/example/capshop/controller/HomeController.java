package com.example.capshop.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@RestController
public class HomeController {
    @GetMapping(value = "/", produces = TEXT_PLAIN_VALUE)
    public String home() {
        // 운영에서는 프론트가 Nginx에서 / 를 처리하는 게 정상입니다.
        // 혹시라도 백엔드로 / 요청이 들어오면 템플릿 렌더링(Thymeleaf) 대신 안전한 응답을 반환합니다.
        return "ok";
    }
}

