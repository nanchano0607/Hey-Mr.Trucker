package com.example.capshop.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundAccountRequest {
    private String refundBank;     // 토스가 요구하는 bank code 권장 (예: "KB", "NH" 등)
    private String refundAccount;  // 계좌번호
    private String refundHolder;   // 예금주
}