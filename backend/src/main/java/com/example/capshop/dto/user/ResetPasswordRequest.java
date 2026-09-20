package com.example.capshop.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String email;
    private String phone;
    private String newPassword;
}
