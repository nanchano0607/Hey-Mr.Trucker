package com.example.capshop.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "story")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String backgroundImage;

    @Column(nullable = false)
    private String contentImage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Story(String backgroundImage, String contentImage) {
        this.backgroundImage = backgroundImage;
        this.contentImage = contentImage;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
