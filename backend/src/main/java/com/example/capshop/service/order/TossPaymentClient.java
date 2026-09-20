package com.example.capshop.service.order;

import java.util.Optional;

import com.example.capshop.dto.order.TossPaymentInfo;

/** 토스페이먼츠 결제 조회. 웹훅 본문을 신뢰하지 않고 서버가 토스에 직접 확인할 때 쓴다. */
public interface TossPaymentClient {

    /**
     * @return 토스에 해당 결제가 없으면 빈 값
     * @throws TossApiUnavailableException 토스를 호출하지 못했거나 응답을 확인할 수 없는 경우
     */
    Optional<TossPaymentInfo> findByPaymentKey(String paymentKey);
}
