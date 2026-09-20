package com.example.capshop.controller.content;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.content.Popup;
import com.example.capshop.service.content.PopupService;

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
}
