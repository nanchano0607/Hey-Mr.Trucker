package com.example.capshop.service.order;

/** 토스 API 를 호출하지 못했거나 응답을 확인할 수 없을 때. 웹훅은 이 경우 재시도되도록 5xx 로 응답한다. */
public class TossApiUnavailableException extends RuntimeException {

    public TossApiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
