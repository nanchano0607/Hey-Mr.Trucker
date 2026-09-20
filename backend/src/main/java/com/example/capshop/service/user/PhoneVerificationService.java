package com.example.capshop.service.user;

import java.time.LocalDateTime;
import java.security.SecureRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.capshop.domain.user.PhoneVerification;
import com.example.capshop.repository.user.PhoneVerificationRepository;
import com.example.capshop.service.common.SolapiSmsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final SolapiSmsService solapiSmsService;

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRE_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 20; // 빠른 재전송 방지
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void sendCode(String phoneNumber) {
        LocalDateTime now = LocalDateTime.now();

        PhoneVerification pv = phoneVerificationRepository.findByPhoneNumber(phoneNumber)
                .orElse(PhoneVerification.builder().phoneNumber(phoneNumber).build());

        // 발송 쿨다운 체크
        if (pv.getLastSentAt() != null && pv.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new IllegalStateException("너무 잦은 요청입니다. 잠시 후 다시 시도하세요.");
        }

        // 새로운 코드 생성
        String code = generateCode();
        pv.setCode(code);
        pv.setCreatedAt(now);
        pv.setExpiresAt(now.plusMinutes(EXPIRE_MINUTES));
        pv.setVerified(false);
        pv.setAttemptCount(0);
        pv.setLastSentAt(now);

        phoneVerificationRepository.save(pv);

        // Solapi SMS 전송
        solapiSmsService.sendVerificationCode(phoneNumber, code);
    }

    @Transactional
    public boolean verifyCode(String phoneNumber, String code) {
        PhoneVerification pv = phoneVerificationRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청이 존재하지 않습니다."));

        if (pv.isVerified()) {
            // 이미 인증된 상태에서도 코드가 맞아야 true (아무 코드나 보내 인증 상태를 확인하는 것을 막는다)
            return pv.getCode() != null && pv.getCode().equals(code);
        }

        if (pv.isExpired()) {
            throw new IllegalStateException("인증 코드가 만료되었습니다.");
        }

        if (pv.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new IllegalStateException("인증 시도 횟수가 초과되었습니다.");
        }

        if (pv.getCode() != null && pv.getCode().equals(code)) {
            pv.markVerified();
            phoneVerificationRepository.save(pv);
            return true;
        } else {
            pv.incrementAttempt();
            phoneVerificationRepository.save(pv);
            return false;
        }
    }

    /** 전화번호가 인증되었고 유효 시간(10분) 안인지 확인한다. */
    public boolean isVerified(String phoneNumber) {
        return phoneVerificationRepository.findByPhoneNumber(phoneNumber)
                .map(verification -> verification.isUsable(LocalDateTime.now()))
                .orElse(false);
    }

    /** 인증을 사용 처리(소멸)한다. 가입·아이디 찾기·비밀번호 재설정이 성공한 뒤 호출해 같은 인증을 다시 쓰지 못하게 한다. */
    @Transactional
    public void consume(String phoneNumber) {
        phoneVerificationRepository.deleteByPhoneNumber(phoneNumber);
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        int v = RANDOM.nextInt(bound);
        return String.format("%0" + CODE_LENGTH + "d", v);
    }
}
