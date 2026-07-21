package com.example.capshop.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.Popup;
import com.example.capshop.service.PopupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PopupController {
    private final PopupService popupService;

    @GetMapping("/popup/current")
    public ResponseEntity<Popup> getCurrentPopup() {
        Popup popup = popupService.getCurrentPopup();
        if (popup == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(popup);
    }

    @GetMapping("/admin/popup")
    public ResponseEntity<Popup> getAdminPopup() {
        return ResponseEntity.ok(popupService.getAdminPopup());
    }

    @PostMapping("/admin/popup")
    public ResponseEntity<Popup> savePopup(@RequestBody Map<String, Object> requestData) {
        String imageUrl = requestData.get("imageUrl") == null ? "" : requestData.get("imageUrl").toString();
        Boolean isActive = requestData.get("isActive") == null
                ? Boolean.FALSE
                : Boolean.valueOf(requestData.get("isActive").toString());

        return ResponseEntity.ok(popupService.save(imageUrl, isActive));
    }

    @PostMapping("/admin/popup/deactivate")
    public ResponseEntity<Popup> deactivatePopup() {
        return ResponseEntity.ok(popupService.deactivate());
    }
}
