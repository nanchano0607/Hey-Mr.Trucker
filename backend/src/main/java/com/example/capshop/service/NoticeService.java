package com.example.capshop.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.capshop.domain.Notice;
import com.example.capshop.dto.NoticeCreateRequest;
import com.example.capshop.dto.NoticeResponse;
import com.example.capshop.dto.NoticeUpdateRequest;
import com.example.capshop.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<NoticeResponse> listAll() {
        List<Notice> notices = noticeRepository.findAllByOrderByCreatedAtDesc();
        return notices.stream().map(NoticeResponse::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoticeResponse getNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        return new NoticeResponse(notice);
    }

    @Transactional
    public NoticeResponse createNotice(Long userId, NoticeCreateRequest req) {
        if (!isAdmin(userId)) {
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        var user = userService.findById(userId);
        String authorName = user.getName() != null ? user.getName() : user.getEmail();
        
        Notice notice = new Notice(req.getTitle(), req.getContent(), authorName);
        Notice saved = noticeRepository.save(notice);
        return new NoticeResponse(saved);
    }

    @Transactional
    public NoticeResponse updateNotice(Long userId, Long id, NoticeUpdateRequest req) {
        if (!isAdmin(userId)) {
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));

        if (req.getTitle() != null) notice.setTitle(req.getTitle());
        if (req.getContent() != null) notice.setContent(req.getContent());
        notice.setUpdatedAt(LocalDateTime.now());
        Notice saved = noticeRepository.save(notice);
        return new NoticeResponse(saved);
    }

    @Transactional
    public void deleteNotice(Long userId, Long id) {
        if (!isAdmin(userId)) {
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }
        noticeRepository.deleteById(id);
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        try {
            var user = userService.findById(userId);
            return user != null && Boolean.TRUE.equals(user.isAdmin());
        } catch (Exception e) {
            return false;
        }
    }
}
