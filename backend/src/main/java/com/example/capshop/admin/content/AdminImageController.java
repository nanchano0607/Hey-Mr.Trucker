package com.example.capshop.admin.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.capshop.service.content.ImageStorageService;
import com.example.capshop.service.content.SiteImageSettings;
import com.example.capshop.service.content.StoryService;
import com.example.capshop.service.product.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminImageController {

    private static final String CAP_URL_PREFIX = "/uploads/" + ImageStorageService.CAP_DIR + "/";

    private final ImageStorageService imageStorage;
    private final SiteImageSettings siteImageSettings;
    private final ProductService productService;
    private final StoryService storyService;

    // =========================
    // 로고/배경 파일명 세팅
    // =========================

    @PostMapping("/logo")
    public Map<String, String> setLogoImage(@RequestParam("filename") String filename) {
        String sanitized = imageStorage.sanitizeFilename(filename);
        siteImageSettings.changeLogoImage(sanitized);
        return Map.of("url", imageStorage.toPublicUrl(CAP_URL_PREFIX + sanitized));
    }

    @PostMapping("/background")
    public Map<String, String> setBackgroundImage(@RequestParam("filename") String filename) {
        String sanitized = imageStorage.sanitizeFilename(filename);
        siteImageSettings.changeBackgroundImage(sanitized);
        return Map.of("url", imageStorage.toPublicUrl(CAP_URL_PREFIX + sanitized));
    }

    // =========================
    // 스토리 배경/콘텐츠
    // =========================

    @PostMapping("/story/background")
    public Map<String, String> setStoryBackground(@RequestParam("filename") String filename) {
        var story = storyService.createOrUpdateStory(imageStorage.sanitizeFilename(filename), null);
        return Map.of("url", imageStorage.toPublicUrl(CAP_URL_PREFIX + story.getBackgroundImage()));
    }

    @PostMapping("/story/content")
    public Map<String, String> setStoryContent(@RequestParam("filename") String filename) {
        var story = storyService.createOrUpdateStory(null, imageStorage.sanitizeFilename(filename));
        return Map.of("url", imageStorage.toPublicUrl(CAP_URL_PREFIX + story.getContentImage()));
    }

    // =========================
    // 업로드 (cap 폴더)
    // =========================

    /** 공용 단일 업로드 */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Map.of("url", imageStorage.store(file, ImageStorageService.CAP_DIR));
    }

    /** 상품 이미지 일괄 업로드: mainImage 1개 + images 여러개 */
    @PostMapping(value = "/product/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadProductImages(
            @RequestParam("mainImage") MultipartFile mainImage,
            @RequestParam("images") List<MultipartFile> images) throws IOException {

        String mainUrl = imageStorage.store(mainImage, ImageStorageService.CAP_DIR);

        return Map.of(
                "mainImageUrl", mainUrl,
                "imageUrls", storeAll(images)
        );
    }

    @PostMapping(value = "/product/upload/main", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadMainImage(
            @RequestParam("mainImage") MultipartFile mainImage,
            @RequestParam("productId") Long productId) throws IOException {

        String mainUrl = imageStorage.store(mainImage, ImageStorageService.CAP_DIR);
        productService.updateMainImageUrl(productId, mainUrl);

        return Map.of("mainImageUrl", mainUrl);
    }

    @PostMapping(value = "/product/upload/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImages(
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("productId") Long productId) throws IOException {

        List<String> imageUrls = storeAll(images);
        productService.updateImageUrls(productId, imageUrls);

        return Map.of("imageUrls", imageUrls);
    }

    // =========================
    // 삭제 (cap/review 모두 처리, 파일명 또는 /uploads/... URL)
    // =========================

    @PostMapping("/image/delete")
    public Map<String, Object> deleteImages(@RequestBody List<String> namesOrUrls) {
        ImageStorageService.DeleteResult result = imageStorage.delete(namesOrUrls);
        return Map.of("success", result.success(), "fail", result.fail());
    }

    private List<String> storeAll(List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();
        if (files == null) {
            return urls;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            urls.add(imageStorage.store(file, ImageStorageService.CAP_DIR));
        }
        return urls;
    }
}
