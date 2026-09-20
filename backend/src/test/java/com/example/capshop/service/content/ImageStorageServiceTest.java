package com.example.capshop.service.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;


class ImageStorageServiceTest {

    @TempDir
    Path uploadDir;

    @Test
    @DisplayName("파일을 저장하면 하위 폴더에 실제 파일이 생기고 /uploads 기준 공개 URL을 돌려준다")
    void store_savesFileAndReturnsPublicUrl() throws IOException {
        // Arrange
        ImageStorageService storage = new ImageStorageService(uploadDir.toString(), "");
        MockMultipartFile file = new MockMultipartFile("file", "hat.png", "image/png",
                "image-bytes".getBytes(StandardCharsets.UTF_8));

        // Act
        String url = storage.store(file, ImageStorageService.CAP_DIR);

        // Assert
        assertThat(url).startsWith("/uploads/cap/").endsWith("_hat.png");
        String storedName = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.readString(uploadDir.resolve("cap").resolve(storedName))).isEqualTo("image-bytes");
    }

    @Test
    @DisplayName("public-base-url이 설정되어 있으면 절대 URL로 조합한다")
    void toPublicUrl_prefixesConfiguredBaseUrl() {
        // Arrange
        ImageStorageService storage = new ImageStorageService(uploadDir.toString(), "http://localhost:8080/");

        // Act
        String url = storage.toPublicUrl("/uploads/cap/a.png");

        // Assert
        assertThat(url).isEqualTo("http://localhost:8080/uploads/cap/a.png");
    }

    @Test
    @DisplayName("삭제는 URL과 상대경로를 모두 지원하고, 업로드 폴더 밖 경로는 실패로 분류한다")
    void delete_removesUploadedFilesAndRejectsPathTraversal() throws IOException {
        // Arrange
        Files.createDirectories(uploadDir.resolve("cap"));
        Path target = Files.writeString(uploadDir.resolve("cap").resolve("old.png"), "x");
        Path outside = Files.writeString(uploadDir.resolveSibling("outside-" + System.nanoTime() + ".txt"), "secret");
        ImageStorageService storage = new ImageStorageService(uploadDir.toString(), "");

        try {
            // Act
            ImageStorageService.DeleteResult result = storage.delete(List.of(
                    "/uploads/cap/old.png",
                    "cap/../../" + outside.getFileName(),
                    "no-folder.png"));

            // Assert
            assertThat(result.success()).containsExactly("/uploads/cap/old.png");
            assertThat(result.fail()).hasSize(2);
            assertThat(target).doesNotExist();
            assertThat(outside).exists();
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    @DisplayName("빈 파일은 저장할 수 없다")
    void store_rejectsEmptyFile() {
        // Arrange
        ImageStorageService storage = new ImageStorageService(uploadDir.toString(), "");
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        // Act & Assert
        assertThatThrownBy(() -> storage.store(empty, ImageStorageService.CAP_DIR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("파일명 정제는 경로 구분자 앞부분을 제거하고 파일명만 남긴다")
    void sanitizeFilename_keepsOnlyFileName() {
        // Arrange
        ImageStorageService storage = new ImageStorageService(uploadDir.toString(), "");

        // Act & Assert
        assertThat(storage.sanitizeFilename("cap/logo.webp")).isEqualTo("logo.webp");
        assertThat(storage.sanitizeFilename("..\\..\\logo.webp")).isEqualTo("logo.webp");
    }
}
