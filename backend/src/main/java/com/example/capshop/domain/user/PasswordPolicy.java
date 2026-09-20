package com.example.capshop.domain.user;

import java.nio.charset.StandardCharsets;

/**
 * 새 비밀번호(회원가입, 재설정)에 적용하는 정책.
 * 영문·숫자·특수문자를 각각 1개 이상 포함한 8자 이상, 공백 불가, UTF-8 72바이트 이하(BCrypt 한계).
 * 로그인에는 적용하지 않는다. 정책 도입 이전에 가입한 회원의 비밀번호도 그대로 유효해야 하기 때문이다.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;
    public static final String GUIDANCE =
            "비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해 " + MIN_LENGTH + "자 이상이어야 합니다.";

    private PasswordPolicy() {
    }

    /** 정책에 맞지 않으면 사용자에게 보여줄 사유를 담아 {@link IllegalArgumentException} 을 던진다. */
    public static void requireValid(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        if (rawPassword.codePoints().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("비밀번호에는 공백을 사용할 수 없습니다.");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "비밀번호가 너무 깁니다. (최대 " + MAX_BYTES + "바이트: 영문·숫자 기준 " + MAX_BYTES + "자, 한글 기준 24자)");
        }
        if (rawPassword.codePointCount(0, rawPassword.length()) < MIN_LENGTH
                || !containsLetter(rawPassword)
                || !containsDigit(rawPassword)
                || !containsSpecial(rawPassword)) {
            throw new IllegalArgumentException(GUIDANCE);
        }
    }

    private static boolean containsLetter(String password) {
        return password.codePoints().anyMatch(cp -> (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z'));
    }

    private static boolean containsDigit(String password) {
        return password.codePoints().anyMatch(cp -> cp >= '0' && cp <= '9');
    }

    /** 문자·숫자·공백이 아닌 문자. 한글은 문자이므로 특수문자로 치지 않는다. */
    private static boolean containsSpecial(String password) {
        return password.codePoints().anyMatch(cp -> !Character.isLetterOrDigit(cp) && !Character.isWhitespace(cp));
    }
}
