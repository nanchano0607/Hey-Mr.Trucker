package com.example.capshop.controller.content;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.dto.content.NoticeResponse;
import com.example.capshop.service.content.NoticeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class NoticeController {

    private final NoticeService noticeService;

    // 공개: 전체 공지 조회
    @GetMapping("/api/notices")
    public ResponseEntity<?> listNotices() {
        
        List<NoticeResponse> list = noticeService.listAll();
        return ResponseEntity.ok(list);
    }

    // 공개: 공지 단건 조회
    @GetMapping("/api/notices/{id}")
    public ResponseEntity<?> getNotice(@PathVariable("id") Long id) {
        try {
            NoticeResponse res = noticeService.getNotice(id);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
