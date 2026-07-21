package com.example.capshop.domain;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "logbook")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Logbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그북 이미지 URL
    @Column(nullable = false)
    private String imageUrl;

    // 정렬 순서 (낮을수록 먼저)
    @Column(nullable = false)
    private Integer sortOrder;

}
