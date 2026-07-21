package com.example.capshop.domain.order;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "checkout")
@Getter
@Setter
public class CheckOut {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String orderId; // ✅ 토스에 넘길 주문번호
	
	private Long userId;    // 주문자 ID

	private String name;      // 수령인
	private String address;   // 배송지 주소
	private String phone;     // 연락처
	
	@Column(columnDefinition = "TEXT")
    private String itemsJson;  // [{"productId":1,"quantity":2,"size":"M"},{"productId":3,"quantity":1}]
   

	private LocalDateTime createdAt;

	public CheckOut() {}

	public CheckOut(String name, String address, String phoneString, String itemsJson) {
		this.name = name;
		this.address = address;
		this.phone = phoneString;
		this.itemsJson = itemsJson;
	}

	@PrePersist
	protected void onCreate() {
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
	}
}
