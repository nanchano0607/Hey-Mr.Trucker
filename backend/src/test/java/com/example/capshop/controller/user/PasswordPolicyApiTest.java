package com.example.capshop.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.capshop.domain.user.AuthProvider;
import com.example.capshop.domain.user.PhoneVerification;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.user.PhoneVerificationRepository;
import com.example.capshop.repository.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordPolicyApiTest {

    private static final String COMPLIANT_PASSWORD = "Valid1234!";
    private static final String LEGACY_WEAK_PASSWORD = "abc";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PhoneVerificationRepository phoneVerificationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"abc123", "abcdefg1", "12345678!", "abcdefgh!", "abc 1234!"})
    @DisplayName("회원가입은 정책에 맞지 않는 비밀번호를 400으로 거부하고 회원을 만들지 않는다")
    void signup_rejectsPasswordViolatingPolicy(String weakPassword) throws Exception {
        // Arrange
        String email = uniqueEmail("signup-weak");
        String phone = verifiedPhone();

        // Act
        String body = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(email, weakPassword, phone)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(body).isNotBlank();
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("회원가입은 정책에 맞는 비밀번호로 가입되고, 그 비밀번호로 로그인할 수 있다")
    void signup_acceptsCompliantPasswordAndAllowsLogin() throws Exception {
        // Arrange
        String email = uniqueEmail("signup-ok");
        String phone = verifiedPhone();

        // Act
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(email, COMPLIANT_PASSWORD, phone)))
                .andExpect(status().isOk());

        // Assert
        assertThat(userRepository.findByEmail(email)).isPresent();
        login(email, COMPLIANT_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("정책 도입 이전에 약한 비밀번호로 가입한 기존 회원도 그대로 로그인할 수 있다")
    void login_existingUserWithWeakPasswordIsUnaffected() throws Exception {
        // Arrange
        User legacyUser = saveLocalUser("legacy", LEGACY_WEAK_PASSWORD, uniquePhone());

        // Act & Assert
        login(legacyUser.getEmail(), LEGACY_WEAK_PASSWORD).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"abc123", "abcdefg1", "abcdefgh!", "abc 1234!"})
    @DisplayName("비밀번호 재설정은 정책에 맞지 않는 새 비밀번호를 400으로 거부하고 기존 비밀번호는 유지된다")
    void resetPassword_rejectsPasswordViolatingPolicy(String weakPassword) throws Exception {
        // Arrange
        String phone = verifiedPhone();
        User user = saveLocalUser("reset-weak", LEGACY_WEAK_PASSWORD, phone);

        // Act
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetJson(user.getEmail(), phone, weakPassword)))
                .andExpect(status().isBadRequest());

        // Assert
        login(user.getEmail(), LEGACY_WEAK_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀번호 재설정은 정책에 맞는 새 비밀번호로 바뀌고 이전 비밀번호로는 로그인할 수 없다")
    void resetPassword_acceptsCompliantPassword() throws Exception {
        // Arrange
        String phone = verifiedPhone();
        User user = saveLocalUser("reset-ok", LEGACY_WEAK_PASSWORD, phone);

        // Act
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetJson(user.getEmail(), phone, COMPLIANT_PASSWORD)))
                .andExpect(status().isOk());

        // Assert
        login(user.getEmail(), COMPLIANT_PASSWORD).andExpect(status().isOk());
        login(user.getEmail(), LEGACY_WEAK_PASSWORD).andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private User saveLocalUser(String prefix, String rawPassword, String phone) {
        return userRepository.save(User.builder()
                .email(uniqueEmail(prefix))
                .password(passwordEncoder.encode(rawPassword))
                .name("정책테스트")
                .phone(phone)
                .oauthProvider(AuthProvider.LOCAL)
                .build());
    }

    private String verifiedPhone() {
        String phone = uniquePhone();
        phoneVerificationRepository.save(PhoneVerification.builder()
                .phoneNumber(phone)
                .code("123456")
                .verified(true)
                .build());
        return phone;
    }

    private String uniquePhone() {
        return String.format("010-8888-%04d", SEQUENCE.incrementAndGet());
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + System.nanoTime() + "@password-policy.test";
    }

    private String signupJson(String email, String password, String phone) {
        return """
                {"email":"%s","password":"%s","name":"정책테스트","phone":"%s",
                 "agreements":{"TERMS":true,"PRIVACY":true}}
                """.formatted(email, password, phone);
    }

    private String resetJson(String email, String phone, String newPassword) {
        return "{\"email\":\"%s\",\"phone\":\"%s\",\"newPassword\":\"%s\"}".formatted(email, phone, newPassword);
    }
}
