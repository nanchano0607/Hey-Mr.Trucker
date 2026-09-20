package com.example.capshop.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhoneVerifyRequest {
    private String phoneNumber;
    private String code;
}
