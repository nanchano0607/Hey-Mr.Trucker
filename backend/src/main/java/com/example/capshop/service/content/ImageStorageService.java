package com.example.capshop.service.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 파일의 저장/삭제와 공개 URL 조합을 담당한다.
 * 저장 위치는 {@code app.upload-dir}, 공개 URL 접두사는 {@code app.public-base-url}.
 */
@Service
public class ImageStorageService {

    public static final String CAP_DIR = "cap";
    public static final String REVIEW_DIR = "review";

    private static final String UPLOADS_URL_PREFIX = "/uploads/";

    private final String uploadBaseDir;
    private final String publicBaseUrl;

    public ImageStorageService(
            @Value("${app.upload-dir}") String uploadBaseDir,
            @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.uploadBaseDir = uploadBaseDir;
        this.publicBaseUrl = Objects.toString(publicBaseUrl, "").trim();
    }

    public record DeleteResult(List<String> success, List<String> fail) {
    }

    public String store(MultipartFile file, String subdir) throws IOException {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("Empty file");
        }

        Path dir = Paths.get(uploadBaseDir, subdir).normalize();
        Files.createDirectories(dir);

        String originalName = Objects.toString(file.getOriginalFilename(), "");
        if (originalName.isBlank()) {
            throw new IllegalArgumentException("Empty filename");
        }

        String filename = System.currentTimeMillis() + "_" + StringUtils.cleanPath(originalName);

        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new IllegalArgumentException("Invalid filename");
        }

        file.transferTo(target.toFile());

        return toPublicUrl(UPLOADS_URL_PREFIX + subdir + "/" + filename);
    }

    /** 파일명 또는 /uploads/... URL 목록을 받아 삭제하고, 성공/실패한 입력값을 구분해 돌려준다. */
    public DeleteResult delete(List<String> namesOrUrls) {
        List<String> success = new ArrayList<>();
        List<String> fail = new ArrayList<>();

        Path base = Paths.get(uploadBaseDir).normalize();

        for (String nameOrUrl : namesOrUrls) {
            try {
                Path target = base.resolve(extractRelativePathUnderUploads(nameOrUrl)).normalize();
                if (!target.startsWith(base)) {
                    fail.add(nameOrUrl);
                    continue;
                }

                if (Files.deleteIfExists(target)) {
                    success.add(nameOrUrl);
                } else {
                    fail.add(nameOrUrl);
                }
            } catch (Exception e) {
                fail.add(nameOrUrl);
            }
        }

        return new DeleteResult(success, fail);
    }

    /** 기본은 상대경로. {@code app.public-base-url}이 있으면 절대 URL로 조합한다. */
    public String toPublicUrl(String path) {
        if (publicBaseUrl.isEmpty()) {
            return path;
        }

        String trimmedPath = Objects.toString(path, "").trim();
        if (trimmedPath.isEmpty()) {
            return publicBaseUrl;
        }

        boolean baseEndsWithSlash = publicBaseUrl.endsWith("/");
        boolean pathStartsWithSlash = trimmedPath.startsWith("/");

        if (baseEndsWithSlash && pathStartsWithSlash) {
            return publicBaseUrl.substring(0, publicBaseUrl.length() - 1) + trimmedPath;
        }
        if (!baseEndsWithSlash && !pathStartsWithSlash) {
            return publicBaseUrl + "/" + trimmedPath;
        }
        return publicBaseUrl + trimmedPath;
    }

    /** 경로 구분자가 포함되어 있으면 마지막 파일명만 남긴다. */
    public String sanitizeFilename(String filename) {
        String clean = StringUtils.cleanPath(Objects.toString(filename, "")).replace("\\", "/");
        return clean.contains("/") ? clean.substring(clean.lastIndexOf('/') + 1) : clean;
    }

    private String extractRelativePathUnderUploads(String input) {
        if (input == null) {
            throw new IllegalArgumentException("null");
        }

        String path = input.trim().replace("\\", "/");

        int uploadsIndex = path.indexOf(UPLOADS_URL_PREFIX);
        if (uploadsIndex >= 0) {
            path = path.substring(uploadsIndex + UPLOADS_URL_PREFIX.length());
        }

        if (!path.startsWith(CAP_DIR + "/") && !path.startsWith(REVIEW_DIR + "/")) {
            throw new IllegalArgumentException("Need path like cap/xxx or review/yyy (or /uploads/...)");
        }

        return path;
    }
}
