package com.example.capshop;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 패키지 구조 규칙: 레이어(controller/service/domain/repository/dto) 아래에는 도메인 하위 패키지만 두고,
 * 관리자 컨트롤러는 최상위 admin 패키지에 도메인별로 모은다.
 */
class PackageStructureTest {

    private static final String BASE_PACKAGE = "com.example.capshop";
    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/example/capshop");
    private static final Set<String> DOMAINS =
            Set.of("user", "product", "cart", "order", "coupon", "review", "content", "common");

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"controller", "service", "domain", "repository", "dto"})
    @DisplayName("레이어 패키지 바로 아래에는 클래스 없이 허용된 도메인 하위 패키지만 존재한다")
    void layer_containsOnlyDomainSubPackages(String layer) throws IOException {
        // Arrange
        Path layerDir = SOURCE_ROOT.resolve(layer);

        // Act
        List<String> looseClasses = listFiles(layerDir);
        List<String> unknownSubPackages = listDirectories(layerDir).stream()
                .filter(name -> !DOMAINS.contains(name))
                .toList();

        // Assert
        assertThat(looseClasses).as("%s 바로 아래에 있는 클래스", layer).isEmpty();
        assertThat(unknownSubPackages).as("%s 아래의 허용되지 않은 하위 패키지", layer).isEmpty();
    }

    @Test
    @DisplayName("관리자 컨트롤러는 최상위 admin 아래 도메인별 하위 패키지에만 존재하고 controller 아래에는 admin이 없다")
    void adminControllers_areGroupedByDomainUnderTopLevelAdminPackage() throws IOException {
        // Arrange
        Path adminDir = SOURCE_ROOT.resolve("admin");

        // Act
        List<String> looseClasses = listFiles(adminDir);
        List<String> unknownSubPackages = listDirectories(adminDir).stream()
                .filter(name -> !DOMAINS.contains(name))
                .toList();
        boolean legacyAdminPackageExists = Files.isDirectory(SOURCE_ROOT.resolve("controller").resolve("admin"));

        // Assert
        assertThat(Files.isDirectory(adminDir)).isTrue();
        assertThat(looseClasses).as("admin 바로 아래에 있는 클래스").isEmpty();
        assertThat(unknownSubPackages).as("admin 아래의 허용되지 않은 하위 패키지").isEmpty();
        assertThat(legacyAdminPackageExists).as("controller/admin 잔존 여부").isFalse();
    }

    @Test
    @DisplayName("모든 소스 파일의 package 선언은 디렉터리 경로와 일치한다")
    void packageDeclaration_matchesDirectory() throws IOException {
        // Arrange
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            sources = walk.filter(path -> path.toString().endsWith(".java")).toList();
        }

        // Act
        List<String> mismatches = sources.stream()
                .filter(path -> !declaredPackage(path).equals(expectedPackage(path)))
                .map(path -> path + " -> " + declaredPackage(path))
                .toList();

        // Assert
        assertThat(sources).isNotEmpty();
        assertThat(mismatches).isEmpty();
    }

    private static List<String> listFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> children = Files.list(dir)) {
            return children.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private static List<String> listDirectories(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> children = Files.list(dir)) {
            return children.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private static String expectedPackage(Path source) {
        Path parent = SOURCE_ROOT.relativize(source).getParent();
        if (parent == null) {
            return BASE_PACKAGE;
        }
        return BASE_PACKAGE + "." + parent.toString().replace('/', '.');
    }

    private static String declaredPackage(Path source) {
        try (Stream<String> lines = Files.lines(source)) {
            return lines.map(String::trim)
                    .filter(line -> line.startsWith("package "))
                    .map(line -> line.substring("package ".length()).replace(";", "").trim())
                    .findFirst()
                    .orElse("");
        } catch (IOException e) {
            throw new IllegalStateException(source + " 을 읽을 수 없습니다.", e);
        }
    }
}
