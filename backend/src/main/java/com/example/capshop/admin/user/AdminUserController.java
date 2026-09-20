package com.example.capshop.admin.user;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.user.User;
import com.example.capshop.dto.user.UserAdminResponse;
import com.example.capshop.service.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    /** 사용자 목록 (DTO로 반환하여 순환 참조/중첩 방지) */
    @GetMapping
    public List<UserAdminResponse> allUser() {
        return userService.findAll().stream()
                .map(UserAdminResponse::new)
                .toList();
    }

    /** 사용자 ID 목록 (프론트 배치 로딩용) */
    @GetMapping("/ids")
    public List<Long> allUserIds() {
        return userService.findAll().stream()
                .map(User::getId)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAdminResponse> getUser(@PathVariable("id") Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new UserAdminResponse(user));
    }

    /** 권한 토글 (승격/해제) */
    @PostMapping("/{id}/toggle-admin")
    public ResponseEntity<Map<String, Object>> toggleAdmin(@PathVariable("id") Long id) {
        try {
            boolean isAdmin = userService.toggleAdmin(id);
            String message = isAdmin ? "관리자 권한이 부여되었습니다." : "관리자 권한이 해제되었습니다.";
            return ResponseEntity.ok(Map.of(
                    "message", message,
                    "admin", isAdmin
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 사용자 상태 토글 (활성화/비활성화) */
    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable("id") Long id) {
        try {
            boolean isDeleted = userService.toggleUserStatus(id);
            String message = isDeleted ? "사용자가 비활성화되었습니다." : "사용자가 활성화되었습니다.";
            return ResponseEntity.ok(Map.of(
                    "message", message,
                    "deleted", isDeleted
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
