package com.example.capshop.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class successController {

    @GetMapping("/success")
    public String showArticles() {
        return "로그인";
    }
}
