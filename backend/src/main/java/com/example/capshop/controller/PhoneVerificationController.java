package com.example.capshop.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.dto.PhoneResponse;
import com.example.capshop.dto.PhoneSendRequest;
import com.example.capshop.dto.PhoneVerifyRequest;
import com.example.capshop.service.PhoneVerificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/phone")
public class PhoneVerificationController {
    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/send")
    public ResponseEntity<PhoneResponse> send(@RequestBody PhoneSendRequest req) {
        try {
            if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
                return ResponseEntity.badRequest().body(new PhoneResponse("전화번호를 입력해주세요."));
            }
            phoneVerificationService.sendCode(req.getPhoneNumber().trim());
            return ResponseEntity.ok(new PhoneResponse("code_sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new PhoneResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<PhoneResponse> verify(@RequestBody PhoneVerifyRequest req) {
        try {
            if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank() || req.getCode() == null || req.getCode().isBlank()) {
                return ResponseEntity.badRequest().body(new PhoneResponse("전화번호와 인증번호를 입력해주세요."));
            }
            boolean ok = phoneVerificationService.verifyCode(req.getPhoneNumber().trim(), req.getCode().trim());
            if (ok) return ResponseEntity.ok(new PhoneResponse("verified"));
            return ResponseEntity.badRequest().body(new PhoneResponse("인증번호가 올바르지 않습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new PhoneResponse(e.getMessage()));
        }
    }
}
