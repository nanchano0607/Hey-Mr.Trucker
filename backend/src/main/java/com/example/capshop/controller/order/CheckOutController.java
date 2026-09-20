package com.example.capshop.controller.order;


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

import com.example.capshop.domain.order.CheckOut;
import com.example.capshop.domain.user.User;
import com.example.capshop.service.order.CheckOutService;
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
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 본문의 id/userId 는 무시하고 로그인한 사용자 소유의 새 체크아웃으로 저장한다.
        CheckOut saved = checkOutService.create(user, body);
        return ResponseEntity
                .created(URI.create("/api/checkout/" + saved.getId()))
                .body(saved);
    }


    @GetMapping("/{id}")
    public ResponseEntity<CheckOutResponse> getCheckout(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User user) {
        CheckOut checkOut = checkOutService.findOwned(id, user)
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
