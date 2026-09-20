package com.example.capshop.admin.content;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.user.User;
import com.example.capshop.dto.content.NoticeCreateRequest;
import com.example.capshop.dto.content.NoticeResponse;
import com.example.capshop.dto.content.NoticeUpdateRequest;
import com.example.capshop.service.content.NoticeService;

import lombok.RequiredArgsConstructor;

/** 관리자 여부는 요청 파라미터가 아니라 /api/admin/** 의 ADMIN 권한으로 판정하고, 작성자는 로그인한 관리자로 기록한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<NoticeResponse> createNotice(
            @AuthenticationPrincipal User admin,
            @RequestBody NoticeCreateRequest req) {
        return ResponseEntity.ok(noticeService.createNotice(admin, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNotice(
            @PathVariable("id") Long id,
            @RequestBody NoticeUpdateRequest req) {
        try {
            return ResponseEntity.ok(noticeService.updateNotice(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotice(@PathVariable("id") Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }
}
