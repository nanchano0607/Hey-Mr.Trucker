package com.example.capshop.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.capshop.domain.Popup;
import com.example.capshop.repository.PopupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupService {
    private final PopupRepository popupRepository;

    /**
     * 관리자 화면에서 사용할 팝업 설정값을 조회한다.
     * 저장된 팝업이 없으면 프론트가 바로 다룰 수 있도록 기본 객체를 반환한다.
     */
    @Transactional(readOnly = true)
    public Popup getAdminPopup() {
        return popupRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> new Popup("", false));
    }

    /**
     * 사용자 화면에 실제로 노출할 현재 팝업을 조회한다.
     * 팝업이 없거나, 비활성 상태이거나, 이미지가 없으면 노출하지 않도록 null을 반환한다.
     */
    @Transactional(readOnly = true)
    public Popup getCurrentPopup() {
        Popup popup = popupRepository.findTopByOrderByIdAsc().orElse(null);
        if (popup == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(popup.getIsActive())) {
            return null;
        }
        if (!StringUtils.hasText(popup.getImageUrl())) {
            return null;
        }
        return popup;
    }

    /**
     * 팝업 이미지와 활성화 여부를 저장하거나 수정한다.
     * 이미지 URL이 비어 있으면 활성화 요청이 와도 비활성 상태로 저장한다.
     */
    @Transactional
    public Popup save(String imageUrl, Boolean isActive) {
        String normalizedImageUrl = StringUtils.hasText(imageUrl) ? imageUrl.trim() : "";
        boolean normalizedActive = Boolean.TRUE.equals(isActive) && StringUtils.hasText(normalizedImageUrl);

        Popup popup = popupRepository.findTopByOrderByIdAsc().orElse(null);
        if (popup == null) {
            popup = new Popup(normalizedImageUrl, normalizedActive);
            return popupRepository.save(popup);
        }

        popup.update(normalizedImageUrl, normalizedActive);
        return popupRepository.save(popup);
    }

    /**
     * 현재 팝업을 유지한 채 노출만 중지한다.
     * 저장된 팝업이 없으면 비활성 기본 팝업을 새로 만든다.
     */
    @Transactional
    public Popup deactivate() {
        Popup popup = popupRepository.findTopByOrderByIdAsc().orElse(null);
        if (popup == null) {
            popup = new Popup("", false);
            return popupRepository.save(popup);
        }

        popup.update(popup.getImageUrl(), false);
        return popupRepository.save(popup);
    }
}
