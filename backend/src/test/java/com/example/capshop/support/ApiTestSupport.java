package com.example.capshop.support;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import com.example.capshop.config.TokenProvider;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.user.UserRepository;

/** MockMvc 기반 API 통합 테스트 공용 지원: 회원 생성, JWT 발급, 운영과 같은 서블릿 경로 설정. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    protected User saveUser(String namePrefix, boolean admin) {
        int seq = SEQUENCE.incrementAndGet();
        return userRepository.save(User.builder()
                .email(namePrefix + "-" + seq + "-" + System.nanoTime() + "@admin-api.test")
                .password("test-password")
                .name(namePrefix + seq)
                .phone(String.format("010-9999-%04d", seq))
                .isAdmin(admin)
                .build());
    }

    protected String bearer(User user) {
        return "Bearer " + tokenProvider.generateToken(user, Duration.ofHours(1));
    }

    /**
     * 운영(Tomcat, DispatcherServlet이 "/"에 매핑)과 동일하게 servletPath에 전체 경로가 채워지도록 한다.
     * MockMvc 기본값(servletPath="")에서는 TokenAuthenticationFilter가 "/api/" 요청으로 인식하지 못한다.
     */
    protected static RequestPostProcessor asDefaultServlet() {
        return request -> {
            request.setServletPath(request.getRequestURI());
            request.setPathInfo(null);
            return request;
        };
    }

    /** 컨트롤러 핸들러가 등록된 (메서드, 경로) 인지 확인한다. 제거한 옛 경로가 남아 있지 않은지 검증할 때 쓴다. */
    protected boolean hasHandler(HttpMethod method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method.name(), path);
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ServletRequestPathUtils.parseAndCache(request);
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            return chain != null && chain.getHandler() instanceof HandlerMethod;
        } catch (HttpRequestMethodNotSupportedException methodNotSupported) {
            return false;
        }
    }
}
