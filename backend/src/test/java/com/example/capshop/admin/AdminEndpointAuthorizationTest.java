package com.example.capshop.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import com.example.capshop.domain.user.User;

import jakarta.servlet.ServletException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminEndpointAuthorizationTest extends AdminApiTestSupport {

    private static final String ADMIN_CONTROLLER_PACKAGE = "com.example.capshop.admin";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    private String adminToken;
    private String userToken;

    @BeforeAll
    void setUpAccounts() {
        User admin = saveUser("authz-admin", true);
        User user = saveUser("authz-user", false);
        adminToken = bearer(admin);
        userToken = bearer(user);
    }

    record Endpoint(HttpMethod method, String path, String contentType) {
        static Endpoint json(HttpMethod method, String path) {
            return new Endpoint(method, path, MediaType.APPLICATION_JSON_VALUE);
        }

        static Endpoint multipart(String path) {
            return new Endpoint(HttpMethod.POST, path, MediaType.MULTIPART_FORM_DATA_VALUE);
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    static Stream<Endpoint> adminEndpoints() {
        Stream<Endpoint> productCommands = Stream.of("cap", "acc", "vintage").flatMap(type -> Stream.of(
                Endpoint.json(HttpMethod.POST, "/api/admin/" + type + "/save"),
                Endpoint.json(HttpMethod.POST, "/api/admin/" + type + "/delete/999999"),
                Endpoint.json(HttpMethod.POST, "/api/admin/" + type + "/setNew/999999"),
                Endpoint.json(HttpMethod.POST, "/api/admin/" + type + "/unsetNew/999999"),
                Endpoint.json(HttpMethod.POST, "/api/admin/" + type + "/updateStock/999999"),
                Endpoint.json(HttpMethod.POST, "/api/admin/" + type + "/updateStock/999999/FREE")));

        Stream<Endpoint> others = Stream.of(
                Endpoint.json(HttpMethod.POST, "/api/admin/product/setNew/999999"),
                Endpoint.json(HttpMethod.POST, "/api/admin/product/unsetNew/999999"),
                Endpoint.json(HttpMethod.PUT, "/api/admin/product/999999/price"),
                Endpoint.json(HttpMethod.GET, "/api/admin/coupons/all"),
                Endpoint.json(HttpMethod.POST, "/api/admin/coupons"),
                Endpoint.json(HttpMethod.DELETE, "/api/admin/coupons/999999"),
                Endpoint.json(HttpMethod.POST, "/api/admin/user-coupons/issue/999999?userId=999999"),
                Endpoint.json(HttpMethod.DELETE, "/api/admin/reviews/999999"),
                Endpoint.json(HttpMethod.POST, "/api/admin/story/background?filename=a.png"),
                Endpoint.json(HttpMethod.POST, "/api/admin/story/content?filename=a.png"),
                Endpoint.json(HttpMethod.POST, "/api/admin/logo?filename=a.png"),
                Endpoint.json(HttpMethod.POST, "/api/admin/background?filename=a.png"),
                Endpoint.multipart("/api/admin/upload"),
                Endpoint.multipart("/api/admin/product/upload"),
                Endpoint.multipart("/api/admin/product/upload/main"),
                Endpoint.multipart("/api/admin/product/upload/images"),
                Endpoint.json(HttpMethod.POST, "/api/admin/image/delete"),
                Endpoint.json(HttpMethod.POST, "/api/admin/logbook"),
                Endpoint.json(HttpMethod.DELETE, "/api/admin/logbook/999999"),
                Endpoint.json(HttpMethod.POST, "/api/admin/notices"),
                Endpoint.json(HttpMethod.PUT, "/api/admin/notices/999999"),
                Endpoint.json(HttpMethod.DELETE, "/api/admin/notices/999999"));

        Stream<Endpoint> alreadyUnderAdminPrefix = Stream.of(
                Endpoint.json(HttpMethod.GET, "/api/admin/orders"),
                Endpoint.json(HttpMethod.GET, "/api/admin/users"),
                Endpoint.json(HttpMethod.GET, "/api/admin/popup"),
                Endpoint.json(HttpMethod.GET, "/api/admin/products"));

        return Stream.of(productCommands, others, alreadyUnderAdminPrefix).flatMap(s -> s);
    }

    static Stream<Endpoint> legacyEndpoints() {
        Stream<Endpoint> productCommands = Stream.of("cap", "acc", "vintage").flatMap(type -> Stream.of(
                Endpoint.json(HttpMethod.POST, "/api/" + type + "/save"),
                Endpoint.json(HttpMethod.POST, "/api/" + type + "/delete/1"),
                Endpoint.json(HttpMethod.POST, "/api/" + type + "/setNew/1"),
                Endpoint.json(HttpMethod.POST, "/api/" + type + "/unsetNew/1"),
                Endpoint.json(HttpMethod.POST, "/api/" + type + "/updateStock/1"),
                Endpoint.json(HttpMethod.POST, "/api/" + type + "/updateStock/1/FREE")));

        Stream<Endpoint> others = Stream.of(
                Endpoint.json(HttpMethod.POST, "/api/product/setNew/1"),
                Endpoint.json(HttpMethod.POST, "/api/product/unsetNew/1"),
                Endpoint.json(HttpMethod.PUT, "/api/product/1/price"),
                Endpoint.json(HttpMethod.GET, "/api/coupons/admin/all"),
                Endpoint.json(HttpMethod.POST, "/api/coupons/admin"),
                Endpoint.json(HttpMethod.DELETE, "/api/coupons/admin/1"),
                Endpoint.json(HttpMethod.POST, "/api/user-coupons/admin/issue/1"),
                Endpoint.json(HttpMethod.DELETE, "/api/reviews/admin/1"),
                Endpoint.json(HttpMethod.POST, "/api/story/background"),
                Endpoint.json(HttpMethod.POST, "/api/story/content"),
                Endpoint.json(HttpMethod.POST, "/api/logo"),
                Endpoint.json(HttpMethod.POST, "/api/background"),
                Endpoint.multipart("/api/upload"),
                Endpoint.multipart("/api/product/upload"),
                Endpoint.multipart("/api/product/upload/main"),
                Endpoint.multipart("/api/product/upload/images"),
                Endpoint.json(HttpMethod.POST, "/api/image/delete"),
                Endpoint.json(HttpMethod.POST, "/api/logbook"),
                Endpoint.json(HttpMethod.DELETE, "/api/logbook/1"),
                Endpoint.json(HttpMethod.POST, "/api/notices"),
                Endpoint.json(HttpMethod.PUT, "/api/notices/1"),
                Endpoint.json(HttpMethod.DELETE, "/api/notices/1"),
                Endpoint.json(HttpMethod.POST, "/api/user/save"));

        return Stream.concat(productCommands, others);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpoints")
    @DisplayName("관리자 엔드포인트는 최상위 admin 패키지(도메인별 하위 패키지)의 컨트롤러가 처리한다")
    void adminEndpoint_isHandledByAdminPackageController(Endpoint endpoint) throws Exception {
        // Arrange & Act
        Optional<HandlerMethod> handler = findHandler(endpoint);

        // Assert
        assertThat(handler).as("%s 를 처리하는 핸들러", endpoint).isPresent();
        assertThat(handler.get().getBeanType().getPackageName()).startsWith(ADMIN_CONTROLLER_PACKAGE + ".");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpoints")
    @DisplayName("비로그인 사용자는 관리자 엔드포인트에서 401을 받는다")
    void anonymous_isUnauthorized(Endpoint endpoint) throws Exception {
        // Arrange
        MockHttpServletRequestBuilder request = requestOf(endpoint);

        // Act & Assert
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpoints")
    @DisplayName("일반 사용자는 관리자 엔드포인트에서 403을 받는다")
    void normalUser_isForbidden(Endpoint endpoint) throws Exception {
        // Arrange
        MockHttpServletRequestBuilder request = requestOf(endpoint).header(HttpHeaders.AUTHORIZATION, userToken);

        // Act & Assert
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpoints")
    @DisplayName("관리자는 보안 필터에서 거부되지 않고 컨트롤러까지 도달한다")
    void admin_reachesController(Endpoint endpoint) throws Exception {
        // Arrange
        MockHttpServletRequestBuilder request = requestOf(endpoint).header(HttpHeaders.AUTHORIZATION, adminToken);

        // Act
        int status = statusOf(request);

        // Assert
        assertThat(status).isNotIn(401, 403);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("legacyEndpoints")
    @DisplayName("기존 관리자용 경로는 더 이상 핸들러가 없다")
    void legacyEndpoint_isNoLongerRegistered(Endpoint endpoint) throws Exception {
        // Arrange & Act
        Optional<HandlerMethod> handler = findHandler(endpoint);

        // Assert
        assertThat(handler).as("%s 를 처리하는 핸들러", endpoint).isEmpty();
    }

    private MockHttpServletRequestBuilder requestOf(Endpoint endpoint) {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(endpoint.method(), endpoint.path())
                .with(asDefaultServlet())
                .contentType(endpoint.contentType());
        if (MediaType.APPLICATION_JSON_VALUE.equals(endpoint.contentType()) && endpoint.method() != HttpMethod.GET) {
            builder.content("{}");
        }
        return builder;
    }

    private int statusOf(MockHttpServletRequestBuilder request) throws Exception {
        try {
            return mockMvc.perform(request).andReturn().getResponse().getStatus();
        } catch (ServletException controllerFailure) {
            // 컨트롤러 내부 예외 = 보안 필터를 통과해 컨트롤러까지 도달했다는 의미
            return 500;
        }
    }

    private Optional<HandlerMethod> findHandler(Endpoint endpoint) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(endpoint.method().name(), pathOf(endpoint));
        request.setContentType(endpoint.contentType());
        ServletRequestPathUtils.parseAndCache(request);
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            return Optional.ofNullable(chain)
                    .map(HandlerExecutionChain::getHandler)
                    .filter(HandlerMethod.class::isInstance)
                    .map(HandlerMethod.class::cast);
        } catch (org.springframework.web.HttpRequestMethodNotSupportedException methodNotSupported) {
            return Optional.empty();
        }
    }

    private String pathOf(Endpoint endpoint) {
        int queryStart = endpoint.path().indexOf('?');
        return queryStart < 0 ? endpoint.path() : endpoint.path().substring(0, queryStart);
    }
}
