package com.example.capshop.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.capshop.domain.user.AuthProvider;
import com.example.capshop.domain.user.PhoneVerification;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.user.PhoneVerificationRepository;
import com.example.capshop.repository.user.UserRepository;
import com.example.capshop.service.user.PhoneVerificationService;
import com.example.capshop.service.user.SocialSignupTokenService;
import com.example.capshop.service.user.UserService;
import com.example.capshop.support.ApiTestSupport;

/**
 * 휴대폰 인증은 짧은 시간(10분) 안에서만 유효하고, 가입·아이디 찾기·비밀번호 재설정에 한 번 쓰면 소멸한다.
 * (한 번 인증한 번호가 영구히 인증된 상태로 남아, 이메일과 전화번호만 알면 SMS 없이 남의 비밀번호를
 * 재설정할 수 있던 문제를 막는다)
 */
class PhoneVerificationSecurityApiTest extends ApiTestSupport {

    private static final String OLD_PASSWORD = "OldPass1234!";
    private static final String NEW_PASSWORD = "NewPass1234!";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PhoneVerificationRepository phoneVerificationRepository;
    @Autowired
    private PhoneVerificationService phoneVerificationService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private SocialSignupTokenService socialSignupTokenService;
    @Autowired
    private UserService userService;

    @Test
    @DisplayName("인증한 지 10분이 지난 번호로는 비밀번호를 재설정할 수 없다")
    void resetPassword_rejectsVerificationOlderThanTenMinutes() throws Exception {
        // Arrange
        String phone = uniquePhone();
        User user = saveLocalUser("stale", phone);
        seedVerification(phone, true, LocalDateTime.now().minusMinutes(11));

        // Act & Assert
        reset(user.getEmail(), phone, NEW_PASSWORD).andExpect(status().isBadRequest());
        login(user.getEmail(), OLD_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증 시각이 없는 과거 데이터(영구 인증 상태)로는 비밀번호를 재설정할 수 없다")
    void resetPassword_rejectsLegacyVerificationWithoutTimestamp() throws Exception {
        // Arrange - 기존 배포에서 verified=true 로만 남아 있던 행
        String phone = uniquePhone();
        User user = saveLocalUser("legacy", phone);
        seedVerification(phone, true, null);

        // Act & Assert
        reset(user.getEmail(), phone, NEW_PASSWORD).andExpect(status().isBadRequest());
        login(user.getEmail(), OLD_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀번호 재설정에 성공하면 인증이 소멸해 같은 인증으로 다시 재설정할 수 없다")
    void resetPassword_consumesVerification() throws Exception {
        // Arrange
        String phone = uniquePhone();
        User user = saveLocalUser("consume-reset", phone);
        seedVerification(phone, true, LocalDateTime.now());

        // Act & Assert
        reset(user.getEmail(), phone, NEW_PASSWORD).andExpect(status().isOk());
        reset(user.getEmail(), phone, "Another1234!").andExpect(status().isBadRequest());
        login(user.getEmail(), NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("정책에 맞지 않는 비밀번호로 실패한 재설정은 인증을 소멸시키지 않아 다시 시도할 수 있다")
    void resetPassword_keepsVerificationWhenPasswordPolicyRejects() throws Exception {
        // Arrange
        String phone = uniquePhone();
        User user = saveLocalUser("retry-reset", phone);
        seedVerification(phone, true, LocalDateTime.now());

        // Act & Assert
        reset(user.getEmail(), phone, "weak").andExpect(status().isBadRequest());
        reset(user.getEmail(), phone, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("아이디 찾기에 성공하면 인증이 소멸한다")
    void findId_consumesVerification() throws Exception {
        // Arrange
        String phone = uniquePhone();
        User user = saveLocalUser("find-id", phone);
        seedVerification(phone, true, LocalDateTime.now());
        String body = "{\"name\":\"%s\",\"phone\":\"%s\"}".formatted(user.getName(), phone);

        // Act & Assert
        mockMvc.perform(post("/api/auth/find-id").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/find-id").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입에 성공하면 인증이 소멸해, 가입 때 쓴 인증으로 그 계정의 비밀번호를 재설정할 수 없다")
    void signup_consumesVerification_soItCannotBeReusedToTakeOverTheAccount() throws Exception {
        // Arrange
        String phone = uniquePhone();
        String email = "signup-" + System.nanoTime() + "@phone-security.test";
        seedVerification(phone, true, LocalDateTime.now());

        // Act
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(email, OLD_PASSWORD, phone)))
                .andExpect(status().isOk());

        // Assert - 가입 직후, SMS 인증 없이 재설정 시도
        reset(email, phone, NEW_PASSWORD).andExpect(status().isBadRequest());
        login(email, OLD_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원가입이 실패하면 인증은 유지되어 다시 가입을 시도할 수 있다")
    void signup_failureKeepsVerification() throws Exception {
        // Arrange
        String phone = uniquePhone();
        String email = "signup-retry-" + System.nanoTime() + "@phone-security.test";
        seedVerification(phone, true, LocalDateTime.now());

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(email, "weak", phone)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(email, OLD_PASSWORD, phone)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("소셜 회원가입도 서버에서 휴대폰 인증을 확인하고, 성공하면 인증이 소멸한다")
    void socialSignup_requiresVerifiedPhoneAndConsumesIt() throws Exception {
        // Arrange
        String phone = uniquePhone();
        String providerUserId = "gid-" + System.nanoTime();
        String token = socialSignupTokenService.createToken("google", providerUserId,
                "social-" + System.nanoTime() + "@phone-security.test", "소셜회원");
        String body = "{\"token\":\"%s\",\"phone\":\"%s\",\"agreements\":{\"TERMS\":true,\"PRIVACY\":true}}"
                .formatted(token, phone);

        // Act & Assert - 인증 없이 시도하면 거부하고 계정을 만들지 않는다
        mockMvc.perform(post("/api/auth/complete-signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(userService.findByProviderAndProviderUserId("google", providerUserId)).isNull();

        // 인증 후에는 가입되고 인증은 소멸한다
        seedVerification(phone, true, LocalDateTime.now());
        mockMvc.perform(post("/api/auth/complete-signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        assertThat(phoneVerificationService.isVerified(phone)).isFalse();
    }

    @Test
    @DisplayName("이미 인증된 번호라도 틀린 코드로 인증 확인을 하면 실패한다")
    void verify_rejectsWrongCodeEvenWhenAlreadyVerified() throws Exception {
        // Arrange
        String phone = uniquePhone();
        seedVerification(phone, true, LocalDateTime.now());

        // Act & Assert
        mockMvc.perform(post("/api/phone/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"%s\",\"code\":\"000000\"}".formatted(phone)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("올바른 코드로 인증하면 10분 동안만 사용할 수 있다")
    void verify_makesPhoneUsableOnlyForTenMinutes() throws Exception {
        // Arrange
        String phone = uniquePhone();
        seedVerification(phone, false, null);

        // Act
        mockMvc.perform(post("/api/phone/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"%s\",\"code\":\"123456\"}".formatted(phone)))
                .andExpect(status().isOk());

        // Assert
        assertThat(phoneVerificationService.isVerified(phone)).isTrue();
        PhoneVerification stored = phoneVerificationRepository.findByPhoneNumber(phone).orElseThrow();
        stored.setVerifiedAt(LocalDateTime.now().minusMinutes(11));
        phoneVerificationRepository.save(stored);
        assertThat(phoneVerificationService.isVerified(phone)).isFalse();
    }

    private ResultActions reset(String email, String phone, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"phone\":\"%s\",\"newPassword\":\"%s\"}".formatted(email, phone, newPassword)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private User saveLocalUser(String prefix, String phone) {
        return userRepository.save(User.builder()
                .email(prefix + "-" + System.nanoTime() + "@phone-security.test")
                .password(passwordEncoder.encode(OLD_PASSWORD))
                .name("인증테스트" + SEQUENCE.incrementAndGet())
                .phone(phone)
                .oauthProvider(AuthProvider.LOCAL)
                .build());
    }

    private void seedVerification(String phone, boolean verified, LocalDateTime verifiedAt) {
        phoneVerificationRepository.save(PhoneVerification.builder()
                .phoneNumber(phone)
                .code("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(verified)
                .verifiedAt(verifiedAt)
                .build());
    }

    private String uniquePhone() {
        return String.format("010-7777-%04d", SEQUENCE.incrementAndGet());
    }

    private String signupJson(String email, String password, String phone) {
        return "{\"email\":\"%s\",\"password\":\"%s\",\"name\":\"인증가입\",\"phone\":\"%s\",\"agreements\":{\"TERMS\":true,\"PRIVACY\":true}}"
                .formatted(email, password, phone);
    }
}
