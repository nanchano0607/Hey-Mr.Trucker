package com.example.capshop.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

import com.example.capshop.domain.order.CheckOut;
import com.example.capshop.repository.CheckOutRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckOutService {

    private final CheckOutRepository checkOutRepository;

    @Transactional
    public CheckOut save(CheckOut checkOut) {
        // 1️⃣ 기본 유효성 검사
        if (checkOut.getName() == null || checkOut.getName().isBlank()) {
            throw new IllegalArgumentException("수령인 이름은 필수입니다.");
        }
        if (checkOut.getAddress() == null || checkOut.getAddress().isBlank()) {
            throw new IllegalArgumentException("주소는 필수입니다.");
        }
        if (checkOut.getPhone() == null || checkOut.getPhone().isBlank()) {
            throw new IllegalArgumentException("연락처는 필수입니다.");
        }
        if (checkOut.getItemsJson() == null || checkOut.getItemsJson().isBlank()) {
            throw new IllegalArgumentException("주문 상품 정보는 필수입니다.");
        }

        // 2️⃣ 우선 1차 저장 (id 자동 생성)
        CheckOut saved = checkOutRepository.save(checkOut);

        // 3️⃣ orderId 생성 (문자열)
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderId = String.format("ORD%s-%06d", date, saved.getId());
        saved.setOrderId(orderId);

        // 4️⃣ 다시 저장 (orderId 반영)
        return checkOutRepository.save(saved);
    }

    public Optional<CheckOut> findById(Long id) {
        return checkOutRepository.findById(id);
    }
    
    public Optional<CheckOut> findByOrderId(String orderId) {
        return checkOutRepository.findByOrderId(orderId);
    }
    
    public void deleteById(Long id) {
        checkOutRepository.deleteById(id);
    }
}
