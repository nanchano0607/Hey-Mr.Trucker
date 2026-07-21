package com.example.capshop.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import com.example.capshop.domain.User;
import com.example.capshop.domain.order.CheckOut;
import com.example.capshop.service.CheckOutService;
import java.net.URI;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checkout")
public class CheckOutController {

    private final CheckOutService checkOutService;

    // 호환: 기존 프론트가 사용하는 /order/save와 REST 스타일 /api/checkout 둘 다 지원
    @PostMapping
    public ResponseEntity<CheckOut> save(
            @AuthenticationPrincipal User user,
            @RequestBody CheckOut body) {
        // 인증된 사용자의 ID를 자동으로 설정
        if (user != null) {
            body.setUserId(user.getId());
        }
        CheckOut saved = checkOutService.save(body);
        return ResponseEntity
                .created(URI.create("/api/checkout/" + saved.getId()))
                .body(saved);
    }


    @GetMapping("/{id}")
    public ResponseEntity<CheckOutResponse> getCheckout(@PathVariable("id") Long id) {
        CheckOut checkOut = checkOutService.findById(id)
                .orElse(null);
        if (checkOut == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            CheckOutResponse dto = new CheckOutResponse(checkOut);
            return ResponseEntity.ok(dto);
        }
        
        @Data
        public static class CheckOutResponse {
            private Long id;
            private String orderId;
            private String name;
            private String address;
            private String phone;
            private String itemsJson;
            private Integer amount; // null 가능
            
            public CheckOutResponse(CheckOut c) {
                this.id = c.getId();
                this.orderId = c.getOrderId();
                this.name = c.getName();
                this.address = c.getAddress();
                this.phone = c.getPhone();
                this.itemsJson = c.getItemsJson();
                // amount는 예시로 null, 추후 Order/OrderItem에서 계산해 넣을 수 있음
                this.amount = null;
            }
        }
}
