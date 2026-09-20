package com.example.capshop.admin;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.capshop.config.TokenProvider;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AdminApiTestSupport {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenProvider tokenProvider;

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
}
