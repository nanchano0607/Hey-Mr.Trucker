package com.example.capshop.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateRequest {
    private int rating;            // 별점 (1~5)
    private String content;        // 리뷰 내용
    private List<String> imageUrls; // 리뷰 이미지 URL 리스트
}
