package com.example.capshop.admin.content;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.content.Popup;
import com.example.capshop.service.content.PopupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/popup")
public class AdminPopupController {

    private final PopupService popupService;

    @GetMapping
    public ResponseEntity<Popup> getPopup() {
        return ResponseEntity.ok(popupService.getAdminPopup());
    }

    @PostMapping
    public ResponseEntity<Popup> savePopup(@RequestBody Map<String, Object> requestData) {
        String imageUrl = requestData.get("imageUrl") == null ? "" : requestData.get("imageUrl").toString();
        Boolean isActive = requestData.get("isActive") == null
                ? Boolean.FALSE
                : Boolean.valueOf(requestData.get("isActive").toString());

        return ResponseEntity.ok(popupService.save(imageUrl, isActive));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Popup> deactivatePopup() {
        return ResponseEntity.ok(popupService.deactivate());
    }
}
