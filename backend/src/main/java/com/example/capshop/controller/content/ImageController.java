package com.example.capshop.controller.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.capshop.service.content.ImageStorageService;
import com.example.capshop.service.content.SiteImageSettings;

import lombok.RequiredArgsConstructor;

/** 공개/일반 사용자용 이미지 API. 관리자용 변경·업로드·삭제는 {@code admin.content.AdminImageController}. */
@RestController
@RequiredArgsConstructor
public class ImageController {

    private static final String CAP_URL_PREFIX = "/uploads/" + ImageStorageService.CAP_DIR + "/";

    private final ImageStorageService imageStorage;
    private final SiteImageSettings siteImageSettings;

    @GetMapping("/api/logo")
    public Map<String, String> getLogoImage() {
        return Map.of("url", imageStorage.toPublicUrl(CAP_URL_PREFIX + siteImageSettings.logoImage()));
    }

    @GetMapping("/api/background")
    public Map<String, String> getBackgroundImage() {
        return Map.of("url", imageStorage.toPublicUrl(CAP_URL_PREFIX + siteImageSettings.backgroundImage()));
    }

    /** 리뷰 이미지 업로드 (review 폴더) */
    @PostMapping(value = "/api/review/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadReviewImages(@RequestParam("images") List<MultipartFile> images) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                imageUrls.add(imageStorage.store(file, ImageStorageService.REVIEW_DIR));
            }
        }

        return Map.of("imageUrls", imageUrls);
    }
}
