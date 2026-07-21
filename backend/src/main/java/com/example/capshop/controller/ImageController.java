package com.example.capshop.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.capshop.service.ProductService;
import com.example.capshop.service.StoryService;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ImageController {
    private final ProductService productService;
    // application-prod.properties:
    // app.upload-dir=/opt/capshop/uploads
    @Value("${app.upload-dir}")
    private String uploadBaseDir;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    private final StoryService storyService;

    private static final String CAP_DIR = "cap";
    private static final String REVIEW_DIR = "review";

    // ✅ 운영에서는 "현재 배경/로고"를 DB에 저장하는 게 맞지만
    // 지금은 기존처럼 메모리 변수로 유지
    private String currentBackground = "mainvideo.webm";
    private String logoImage = "homelogo.webp";

    // =========================
    // 로고/배경 파일명 세팅
    // =========================

    @GetMapping("/api/logo")
    public Map<String, String> getLogoImage() {
        // ✅ 기본은 상대경로. 로컬에서 cross-origin 테스트가 필요하면 app.public-base-url로 절대 URL 반환.
        return Map.of("url", toPublicUrl("/uploads/" + CAP_DIR + "/" + logoImage));
    }

    @PostMapping("/api/logo")
    public Map<String, String> setLogoImage(@RequestParam("filename") String filename) {
        this.logoImage = sanitizeFilename(filename);
        return Map.of("url", toPublicUrl("/uploads/" + CAP_DIR + "/" + this.logoImage));
    }

    @GetMapping("/api/background")
    public Map<String, String> getBackgroundImage() {
        return Map.of("url", toPublicUrl("/uploads/" + CAP_DIR + "/" + currentBackground));
    }

    @PostMapping("/api/background")
    public Map<String, String> setBackgroundImage(@RequestParam("filename") String filename) {
        this.currentBackground = sanitizeFilename(filename);
        return Map.of("url", toPublicUrl("/uploads/" + CAP_DIR + "/" + this.currentBackground));
    }

    // =========================
    // 스토리 배경/콘텐츠
    // =========================



    @PostMapping("/api/story/background")
    public Map<String, String> setStoryBackground(@RequestParam("filename") String filename) {
        String sanitized = sanitizeFilename(filename);
        var story = storyService.createOrUpdateStory(sanitized, null);
        return Map.of("url", toPublicUrl("/uploads/" + CAP_DIR + "/" + story.getBackgroundImage()));
    }

    @PostMapping("/api/story/content")
    public Map<String, String> setStoryContent(@RequestParam("filename") String filename) {
        String sanitized = sanitizeFilename(filename);
        var story = storyService.createOrUpdateStory(null, sanitized);
        return Map.of("url", toPublicUrl("/uploads/" + CAP_DIR + "/" + story.getContentImage()));
    }

    // =========================
    // 공용 단일 업로드 (cap 폴더)
    // =========================

    @PostMapping(value = "/api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String url = saveToSubdir(file, CAP_DIR);
        return Map.of("url", url);
    }

    // =========================
    // 상품 이미지 일괄 업로드
    // mainImage 1개 + images 여러개 (모두 cap 폴더)
    // =========================

    @PostMapping(value = "/api/product/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadCapImages(
            @RequestParam("mainImage") MultipartFile mainImage,
            @RequestParam("images") List<MultipartFile> images
    ) throws IOException {

        String mainUrl = saveToSubdir(mainImage, CAP_DIR);

        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile f : images) {
                if (f == null || f.isEmpty()) continue;
                imageUrls.add(saveToSubdir(f, CAP_DIR));
            }
        }

        return Map.of(
                "mainImageUrl", mainUrl,
                "imageUrls", imageUrls
        );
    }
@PostMapping(value = "/api/product/upload/main", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Map<String, String> uploadMainImage(
        @RequestParam("mainImage") MultipartFile mainImage,
        @RequestParam("productId") Long productId
) throws IOException {

    String mainUrl = saveToSubdir(mainImage, CAP_DIR);

    // DB에 메인 이미지 저장
    productService.updateMainImageUrl(productId, mainUrl);

    return Map.of("mainImageUrl", mainUrl);
}

@PostMapping(value = "/api/product/upload/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Map<String, Object> uploadProductImages(
        @RequestParam("images") List<MultipartFile> images,
        @RequestParam("productId") Long productId
) throws IOException {

    List<String> imageUrls = new ArrayList<>();

    if (images != null) {
        for (MultipartFile f : images) {
            if (f == null || f.isEmpty()) continue;
            imageUrls.add(saveToSubdir(f, CAP_DIR));
        }
    }

    // DB에 상품 이미지들 저장
    productService.updateImageUrls(productId, imageUrls);

    return Map.of("imageUrls", imageUrls);
}
    // =========================
    // 리뷰 이미지 업로드 (review 폴더)
    // =========================

    @PostMapping(value = "/api/review/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadReviewImages(@RequestParam("images") List<MultipartFile> images) throws IOException {

        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                imageUrls.add(saveToSubdir(file, REVIEW_DIR));
            }
        }

        return Map.of("imageUrls", imageUrls);
    }

    // =========================
    // 삭제 (cap/review 모두 처리 가능)
    // - 프론트/DB가 파일명만 보내든
    // - "/uploads/cap/xxx.png" 같은 URL을 보내든
    // 둘 다 삭제되도록 처리
    // =========================

    @PostMapping("/api/image/delete")
    public Map<String, Object> deleteImages(@RequestBody List<String> namesOrUrls) {
        List<String> success = new ArrayList<>();
        List<String> fail = new ArrayList<>();

        Path base = Paths.get(uploadBaseDir).normalize();

        for (String s : namesOrUrls) {
            try {
                String relative = extractRelativePathUnderUploads(s); // "cap/xxx.png" or "review/yyy.png"

                Path p = base.resolve(relative).normalize();
                if (!p.startsWith(base)) {
                    fail.add(s);
                    continue;
                }

                if (Files.deleteIfExists(p)) success.add(s);
                else fail.add(s);

            } catch (Exception e) {
                fail.add(s);
            }
        }

        return Map.of("success", success, "fail", fail);
    }

    // =========================
    // 내부 유틸
    // =========================

    private String saveToSubdir(MultipartFile file, String subdir) throws IOException {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("Empty file");
        }

        Path dir = Paths.get(uploadBaseDir, subdir).normalize();
        Files.createDirectories(dir);

        String originalName = Objects.toString(file.getOriginalFilename(), "");
        if (originalName.isBlank()) {
            throw new IllegalArgumentException("Empty filename");
        }

        String original = StringUtils.cleanPath(originalName);
        String filename = System.currentTimeMillis() + "_" + original;

        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new IllegalArgumentException("Invalid filename");
        }

        java.io.File targetFile = target.toFile();
        file.transferTo(targetFile);

        // ✅ 기본은 상대경로. 필요 시 app.public-base-url로 절대 URL 반환.
        return toPublicUrl("/uploads/" + subdir + "/" + filename);
    }

    private String toPublicUrl(String path) {
        String base = Objects.toString(publicBaseUrl, "").trim();
        if (base.isEmpty()) return path;

        String p = Objects.toString(path, "").trim();
        if (p.isEmpty()) return base;

        boolean baseEndsWithSlash = base.endsWith("/");
        boolean pathStartsWithSlash = p.startsWith("/");

        if (baseEndsWithSlash && pathStartsWithSlash) return base.substring(0, base.length() - 1) + p;
        if (!baseEndsWithSlash && !pathStartsWithSlash) return base + "/" + p;
        return base + p;
    }

    private String sanitizeFilename(String filename) {
        String clean = StringUtils.cleanPath(Objects.toString(filename, ""));
        // "cap/xxx.png" 같이 들어오면 파일명만 남기거나, 아예 거부할 수도 있음.
        // 지금은 파일명만 받는다고 가정.
        clean = clean.replace("\\", "/");
        if (clean.contains("/")) clean = clean.substring(clean.lastIndexOf('/') + 1);
        return clean;
    }

    private String extractRelativePathUnderUploads(String input) {
        if (input == null) throw new IllegalArgumentException("null");

        String s = input.trim().replace("\\", "/");

        // 전체 URL이면 /uploads/ 이후만 잘라냄
        int idx = s.indexOf("/uploads/");
        if (idx >= 0) s = s.substring(idx + "/uploads/".length());

        // 이미 상대경로면 그대로 사용 가능: "cap/xxx.png"
        // 파일명만 들어오면 어디 폴더인지 모르므로 실패 처리하는 게 안전
        // (원하면 "cap" 기본으로 처리 가능)
        if (!s.startsWith("cap/") && !s.startsWith("review/")) {
            throw new IllegalArgumentException("Need path like cap/xxx or review/yyy (or /uploads/...)");
        }

        return s;
    }
}
