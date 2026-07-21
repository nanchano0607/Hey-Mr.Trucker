package com.example.capshop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteReturnRequest {
    private Long returnShippingFee;  // 반품 배송비 (차감할 금액)
    private String returnReason;    // 반품 사유 (선택사항)
}
