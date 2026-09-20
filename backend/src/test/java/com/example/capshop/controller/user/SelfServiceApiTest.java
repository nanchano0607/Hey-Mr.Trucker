package com.example.capshop.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.capshop.domain.coupon.Coupon;
import com.example.capshop.domain.coupon.CouponType;
import com.example.capshop.domain.coupon.UserCoupon;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.coupon.CouponRepository;
import com.example.capshop.repository.coupon.UserCouponRepository;
import com.example.capshop.repository.user.UserRepository;
import com.example.capshop.support.ApiTestSupport;

/**
 * 프로필·주소·포인트·쿠폰 API 는 클라이언트가 보낸 id 가 아니라 로그인한 사용자 본인만 대상으로 한다.
 * (경로에 사용자 id 를 받는 옛 API 는 제거되었다)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelfServiceApiTest extends ApiTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private UserCouponRepository userCouponRepository;

    private User alice;
    private User bob;
    private String aliceToken;
    private String bobToken;

    @BeforeAll
    void setUpAccounts() {
        alice = saveUser("self-alice", false);
        alice.addPoints(3_000L);
        alice = userRepository.save(alice);
        bob = saveUser("self-bob", false);
        bob.addPoints(1_000L);
        bob = userRepository.save(bob);
        aliceToken = bearer(alice);
        bobToken = bearer(bob);

        Coupon coupon = couponRepository.save(new Coupon("셀프 서비스 쿠폰 " + System.nanoTime(),
                "SELF" + System.nanoTime(), CouponType.AMOUNT, 3_000, null, null, "테스트"));
        userCouponRepository.save(new UserCoupon(alice, coupon));
    }

    @Test
    @DisplayName("내 프로필 조회는 로그인한 사용자 본인의 정보만 돌려준다")
    void getMyProfile_returnsOnlyOwnData() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/user/me").header(HttpHeaders.AUTHORIZATION, aliceToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(alice.getEmail()));
        mockMvc.perform(get("/api/user/me").header(HttpHeaders.AUTHORIZATION, bobToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(bob.getEmail()));
    }

    @Test
    @DisplayName("내 정보 수정은 본인만 바뀌고 다른 사용자는 그대로다")
    void updateMyProfile_changesOnlyOwnAccount() throws Exception {
        // Arrange
        String newName = "수정된이름" + System.nanoTime();
        String bobNameBefore = userRepository.findById(bob.getId()).orElseThrow().getName();

        // Act
        mockMvc.perform(post("/api/user/me/update")
                .header(HttpHeaders.AUTHORIZATION, aliceToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\"}".formatted(newName)))
                .andExpect(status().isOk());

        // Assert
        assertThat(userRepository.findById(alice.getId()).orElseThrow().getName()).isEqualTo(newName);
        assertThat(userRepository.findById(bob.getId()).orElseThrow().getName()).isEqualTo(bobNameBefore);
    }

    @Test
    @DisplayName("배송지는 로그인한 사용자별로 따로 저장·조회·삭제된다")
    void addresses_areScopedToLoggedInUser() throws Exception {
        // Arrange
        String address = "서울시 테스트구 " + System.nanoTime();

        // Act - alice 가 추가
        mockMvc.perform(post("/api/user/me/addresses")
                .header(HttpHeaders.AUTHORIZATION, aliceToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"address\":\"%s\"}".formatted(address)))
                .andExpect(status().isOk());

        // Assert - alice 에게만 보인다
        String aliceView = mockMvc.perform(get("/api/user/me/addresses")
                .header(HttpHeaders.AUTHORIZATION, aliceToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String bobView = mockMvc.perform(get("/api/user/me/addresses")
                .header(HttpHeaders.AUTHORIZATION, bobToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(aliceView).contains(address);
        assertThat(bobView).doesNotContain(address);

        // Act - 삭제
        mockMvc.perform(post("/api/user/me/addresses/remove")
                .header(HttpHeaders.AUTHORIZATION, aliceToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"address\":\"%s\"}".formatted(address)))
                .andExpect(status().isOk());
        String afterRemove = mockMvc.perform(get("/api/user/me/addresses")
                .header(HttpHeaders.AUTHORIZATION, aliceToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(afterRemove).doesNotContain(address);
    }

    @Test
    @DisplayName("포인트 조회는 본인의 포인트만 돌려준다")
    void getMyPoints_returnsOnlyOwnPoints() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/points/me").header(HttpHeaders.AUTHORIZATION, aliceToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(3_000));
        mockMvc.perform(get("/api/points/me").header(HttpHeaders.AUTHORIZATION, bobToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(1_000));
    }

    @Test
    @DisplayName("쿠폰 조회는 본인의 쿠폰만 돌려준다")
    void getMyCoupons_returnsOnlyOwnCoupons() throws Exception {
        // Act & Assert - alice 는 1장, bob 은 0장
        for (String path : new String[] {"/api/user-coupons/me", "/api/user-coupons/me/available"}) {
            mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, aliceToken).with(asDefaultServlet()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
            mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bobToken).with(asDefaultServlet()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
        mockMvc.perform(get("/api/user-coupons/me/applicable").param("orderAmount", "50000")
                .header(HttpHeaders.AUTHORIZATION, aliceToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/user-coupons/me/applicable").param("orderAmount", "50000")
                .header(HttpHeaders.AUTHORIZATION, bobToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("회원 탈퇴는 로그인한 본인 계정만 탈퇴시킨다")
    void deleteMyAccount_affectsOnlyLoggedInUser() throws Exception {
        // Arrange
        User leaver = saveUser("self-leaver", false);
        User stayer = saveUser("self-stayer", false);

        // Act
        mockMvc.perform(post("/api/user/me/delete")
                .header(HttpHeaders.AUTHORIZATION, bearer(leaver)).with(asDefaultServlet()))
                .andExpect(status().isOk());

        // Assert
        assertThat(userRepository.findById(leaver.getId()).orElseThrow().isDeleted()).isTrue();
        assertThat(userRepository.findById(stayer.getId()).orElseThrow().isDeleted()).isFalse();
    }

    static Stream<String[]> legacyEndpoints() {
        return Stream.of(
                new String[] {"GET", "/api/user/1"},
                new String[] {"POST", "/api/user/1/update"},
                new String[] {"POST", "/api/user/1/delete"},
                new String[] {"GET", "/api/user/1/addresses"},
                new String[] {"POST", "/api/user/1/addresses"},
                new String[] {"POST", "/api/user/1/addresses/remove"},
                new String[] {"GET", "/api/points/user/1"},
                new String[] {"GET", "/api/user-coupons/user/1"},
                new String[] {"GET", "/api/user-coupons/user/1/available"},
                new String[] {"GET", "/api/user-coupons/user/1/applicable"});
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("legacyEndpoints")
    @DisplayName("사용자 id 를 경로로 받던 옛 API 는 더 이상 존재하지 않는다")
    void legacyEndpoint_isNoLongerRegistered(String method, String path) throws Exception {
        // Act & Assert
        assertThat(hasHandler(HttpMethod.valueOf(method), path)).as("%s %s", method, path).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("selfEndpoints")
    @DisplayName("로그인하지 않으면 내 정보 API 는 401을 돌려준다")
    void selfEndpoint_requiresLogin(String path) throws Exception {
        // Act & Assert
        mockMvc.perform(get(path).with(asDefaultServlet())).andExpect(status().isUnauthorized());
    }

    static Stream<String> selfEndpoints() {
        return Stream.of("/api/user/me", "/api/user/me/addresses", "/api/points/me",
                "/api/user-coupons/me", "/api/user-coupons/me/available");
    }
}
