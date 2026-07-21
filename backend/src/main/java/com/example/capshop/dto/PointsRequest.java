package com.example.capshop.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PointsRequest {
    private Long userId;
    private Long amount;
    private String reason; // 적립/사용 사유 (예: "주문 적립", "적립금 사용")
}