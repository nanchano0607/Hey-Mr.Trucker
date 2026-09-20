package com.example.capshop.service.content;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

/**
 * 현재 사이트에 노출 중인 로고/배경 파일명.
 * 조회(공개)와 변경(관리자) 컨트롤러가 함께 쓰므로 스레드 안전하게 보관한다.
 * 운영에서는 DB 저장이 맞지만 기존처럼 메모리 유지 (재시작 시 기본값으로 초기화).
 */
@Component
public class SiteImageSettings {

    private final AtomicReference<String> backgroundImage = new AtomicReference<>("mainvideo.webm");
    private final AtomicReference<String> logoImage = new AtomicReference<>("homelogo.webp");

    public String backgroundImage() {
        return backgroundImage.get();
    }

    public void changeBackgroundImage(String filename) {
        backgroundImage.set(filename);
    }

    public String logoImage() {
        return logoImage.get();
    }

    public void changeLogoImage(String filename) {
        logoImage.set(filename);
    }
}
