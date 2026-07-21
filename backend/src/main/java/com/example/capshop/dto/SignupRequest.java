package com.example.capshop.dto;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String email;
    private String password;
    private String name;
    private String phone;
    private Map<String, Boolean> agreements; // 동의 정보 추가
}
