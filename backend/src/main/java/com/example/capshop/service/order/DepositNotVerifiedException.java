package com.example.capshop.service.order;

/** 웹훅이 알려준 입금 완료를 토스에서 확인하지 못했을 때(위조 웹훅 등). */
public class DepositNotVerifiedException extends RuntimeException {

    public DepositNotVerifiedException(String message) {
        super(message);
    }
}
