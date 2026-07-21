package com.example.capshop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.dto.NoticeCreateRequest;
import com.example.capshop.dto.NoticeResponse;
import com.example.capshop.dto.NoticeUpdateRequest;
import com.example.capshop.service.NoticeService;

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

    // 관리자: 공지 생성 (userId required, 관리자 권한 확인)
    @PostMapping("/api/notices")
    public ResponseEntity<?> createNotice(
            @RequestParam(name = "userId") Long userId,
            @RequestBody NoticeCreateRequest req) {
        try {
            NoticeResponse res = noticeService.createNotice(userId, req);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    // 관리자: 공지 수정
    @PutMapping("/api/notices/{id}")
    public ResponseEntity<?> updateNotice(
            @PathVariable("id") Long id,
            @RequestParam(name = "userId") Long userId,
            @RequestBody NoticeUpdateRequest req) {
        try {
            NoticeResponse res = noticeService.updateNotice(userId, id, req);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    // 관리자: 공지 삭제
    @DeleteMapping("/api/notices/{id}")
    public ResponseEntity<?> deleteNotice(
            @PathVariable("id") Long id,
            @RequestParam(name = "userId") Long userId) {
        try {
            noticeService.deleteNotice(userId, id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
