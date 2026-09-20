package com.example.capshop.domain.user;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    static Stream<String> validPasswords() {
        return Stream.of(
                "abcd1234!",
                "Abcdef1@",
                "a1!aaaaa",                       // 정확히 8자
                "12345678a!",
                "비밀번호a1!x",                     // 한글은 허용되지만 영문/숫자/특수문자는 별도로 필요
                "Password1★",                     // 비ASCII 기호도 특수문자로 인정
                "a1!" + "a".repeat(69)            // 정확히 72바이트
        );
    }

    static Stream<String> passwordsMissingRequiredCharacterType() {
        return Stream.of(
                "12345678!",                      // 영문 없음
                "!!!!1111",                       // 영문 없음
                "비밀번호12345!",                   // 한글은 영문으로 인정하지 않음
                "abcdefgh!",                      // 숫자 없음
                "abcd12345",                      // 특수문자 없음
                "Abcd1234"                        // 특수문자 없음
        );
    }

    static Stream<String> passwordsWithWhitespace() {
        return Stream.of("abcd 1234!", " abcd1234!", "abcd1234! ", "abcd\t1234!");
    }

    static Stream<String> passwordsExceedingMaxBytes() {
        return Stream.of(
                "a1!" + "a".repeat(70),           // 73바이트
                "a1!" + "가".repeat(24)            // 27자지만 UTF-8 75바이트
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validPasswords")
    @DisplayName("영문·숫자·특수문자를 각각 1개 이상 포함한 8자 이상 비밀번호는 통과한다")
    void requireValid_acceptsCompliantPassword(String password) {
        // Arrange & Act & Assert
        assertThatCode(() -> PasswordPolicy.requireValid(password)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"a1!", "ab1!xyz", "abc123!"})
    @DisplayName("8자 미만이면 거부한다")
    void requireValid_rejectsTooShortPassword(String password) {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> PasswordPolicy.requireValid(password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8자 이상");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("passwordsMissingRequiredCharacterType")
    @DisplayName("영문, 숫자, 특수문자 중 하나라도 없으면 거부한다")
    void requireValid_rejectsPasswordMissingCharacterType(String password) {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> PasswordPolicy.requireValid(password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("영문, 숫자, 특수문자");
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("passwordsWithWhitespace")
    @DisplayName("공백(앞뒤 포함)이 들어 있으면 거부한다")
    void requireValid_rejectsWhitespace(String password) {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> PasswordPolicy.requireValid(password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공백");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("passwordsExceedingMaxBytes")
    @DisplayName("UTF-8 기준 72바이트를 넘으면 거부한다")
    void requireValid_rejectsPasswordLongerThanBcryptLimit(String password) {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> PasswordPolicy.requireValid(password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("72");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("null이거나 비어 있으면 거부한다")
    void requireValid_rejectsNullOrBlank(String password) {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> PasswordPolicy.requireValid(password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("입력");
    }

    @Test
    @DisplayName("안내 문구는 규칙 전체(영문·숫자·특수문자, 8자 이상)를 설명한다")
    void guidance_describesWholePolicy() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> PasswordPolicy.requireValid("abcd1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PasswordPolicy.GUIDANCE);
    }
}
