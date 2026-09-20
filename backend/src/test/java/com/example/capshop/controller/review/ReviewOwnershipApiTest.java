package com.example.capshop.controller.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.capshop.domain.order.Order;
import com.example.capshop.domain.order.OrderItem;
import com.example.capshop.domain.order.Status;
import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.review.Review;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.order.OrderRepository;
import com.example.capshop.repository.product.ProductRepository;
import com.example.capshop.repository.review.ReviewRepository;
import com.example.capshop.support.ApiTestSupport;

/** 리뷰 작성·수정·삭제·조회는 요청에 담긴 userId 가 아니라 로그인한 사용자를 기준으로 한다. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReviewOwnershipApiTest extends ApiTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    private User alice;
    private User bob;
    private String aliceToken;
    private String bobToken;
    private Product product;

    @BeforeAll
    void setUp() {
        alice = saveUser("review-alice", false);
        bob = saveUser("review-bob", false);
        aliceToken = bearer(alice);
        bobToken = bearer(bob);

        Product newProduct = new Product();
        newProduct.setName("리뷰 소유권 테스트 캡 " + System.nanoTime());
        newProduct.setPrice(20_000L);
        product = productRepository.save(newProduct);
    }

    @Test
    @DisplayName("리뷰 작성자는 본문의 userId 가 아니라 로그인한 사용자로 기록된다")
    void create_usesLoggedInUserAsAuthor() throws Exception {
        // Arrange
        Order order = saveDeliveredOrder(alice);
        String content = "작성자 검증 리뷰 " + System.nanoTime();

        // Act - alice 가 본문에 bob 의 userId 를 넣어 작성
        mockMvc.perform(post("/api/reviews")
                .header(HttpHeaders.AUTHORIZATION, aliceToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson(bob.getId(), order, content)))
                .andExpect(status().isOk());

        // Assert
        Review saved = findByContent(content);
        assertThat(saved.getUser().getId()).isEqualTo(alice.getId());
    }

    @Test
    @DisplayName("남의 주문에는 그 주문자의 userId 를 담아도 리뷰를 작성할 수 없다")
    void create_rejectsOtherUsersOrderEvenWithForgedUserId() throws Exception {
        // Arrange
        Order aliceOrder = saveDeliveredOrder(alice);
        String content = "위조 리뷰 " + System.nanoTime();

        // Act - bob 이 alice 의 userId 를 넣어 alice 의 주문에 작성 시도
        mockMvc.perform(post("/api/reviews")
                .header(HttpHeaders.AUTHORIZATION, bobToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson(alice.getId(), aliceOrder, content)))
                .andExpect(status().isBadRequest());

        // Assert
        assertThat(reviewRepository.findAll()).extracting(Review::getContent).doesNotContain(content);
    }

    @Test
    @DisplayName("남의 리뷰는 작성자의 userId 를 파라미터로 넘겨도 수정할 수 없고 작성자는 수정할 수 있다")
    void update_onlyByAuthor() throws Exception {
        // Arrange
        Review review = saveReview(alice, "원본 리뷰 " + System.nanoTime());

        // Act & Assert - bob 이 alice 의 userId 를 파라미터로 넘겨 수정 시도
        mockMvc.perform(put("/api/reviews/{id}", review.getId()).param("userId", String.valueOf(alice.getId()))
                .header(HttpHeaders.AUTHORIZATION, bobToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":1,\"content\":\"변조\"}"))
                .andExpect(status().isBadRequest());
        assertThat(reviewRepository.findById(review.getId()).orElseThrow().getContent()).startsWith("원본 리뷰");

        // 작성자 본인은 userId 없이 수정 가능
        mockMvc.perform(put("/api/reviews/{id}", review.getId())
                .header(HttpHeaders.AUTHORIZATION, aliceToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":4,\"content\":\"수정됨\"}"))
                .andExpect(status().isOk());
        assertThat(reviewRepository.findById(review.getId()).orElseThrow().getContent()).isEqualTo("수정됨");
    }

    @Test
    @DisplayName("남의 리뷰는 작성자의 userId 를 파라미터로 넘겨도 삭제할 수 없고 작성자는 삭제할 수 있다")
    void delete_onlyByAuthor() throws Exception {
        // Arrange
        Review review = saveReview(alice, "삭제 대상 리뷰 " + System.nanoTime());

        // Act & Assert
        mockMvc.perform(delete("/api/reviews/{id}", review.getId()).param("userId", String.valueOf(alice.getId()))
                .header(HttpHeaders.AUTHORIZATION, bobToken).with(asDefaultServlet()))
                .andExpect(status().isBadRequest());
        assertThat(reviewRepository.findById(review.getId())).isPresent();

        mockMvc.perform(delete("/api/reviews/{id}", review.getId())
                .header(HttpHeaders.AUTHORIZATION, aliceToken).with(asDefaultServlet()))
                .andExpect(status().isOk());
        assertThat(reviewRepository.findById(review.getId())).isEmpty();
    }

    @Test
    @DisplayName("내 리뷰 목록은 로그인한 사용자의 리뷰만 돌려주고 로그인이 필요하다")
    void myReviews_returnsOnlyOwnAndRequiresLogin() throws Exception {
        // Arrange
        String marker = "내리뷰목록 " + System.nanoTime();
        saveReview(alice, marker);

        // Act
        String aliceView = mockMvc.perform(get("/api/reviews/me")
                .header(HttpHeaders.AUTHORIZATION, aliceToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String bobView = mockMvc.perform(get("/api/reviews/me")
                .header(HttpHeaders.AUTHORIZATION, bobToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(aliceView).contains(marker);
        assertThat(bobView).doesNotContain(marker);
        mockMvc.perform(get("/api/reviews/me").with(asDefaultServlet())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 작성 여부 확인은 주문자만 할 수 있고 타인은 404를 받는다")
    void checkReviewExists_onlyForOrderOwner() throws Exception {
        // Arrange
        Order order = saveDeliveredOrder(alice);

        // Act & Assert
        mockMvc.perform(get("/api/reviews/check")
                .param("orderId", String.valueOf(order.getId())).param("productId", String.valueOf(product.getId()))
                .header(HttpHeaders.AUTHORIZATION, aliceToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canWrite").value(true))
                .andExpect(jsonPath("$.alreadyReviewed").value(false));
        mockMvc.perform(get("/api/reviews/check")
                .param("orderId", String.valueOf(order.getId())).param("productId", String.valueOf(product.getId()))
                .header(HttpHeaders.AUTHORIZATION, bobToken).with(asDefaultServlet()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("사용자 id 를 경로로 받던 리뷰 조회 API 는 더 이상 존재하지 않는다")
    void legacyReviewsByUserId_isNoLongerRegistered() throws Exception {
        // Act & Assert
        assertThat(hasHandler(HttpMethod.GET, "/api/reviews/user/1")).isFalse();
    }

    private Order saveDeliveredOrder(User orderer) {
        Order order = new Order(orderer);
        order.setOrderId("ORD-REV-" + System.nanoTime());
        order.setStatus(Status.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        order.setReceiverName("수령인");
        order.setAddress("서울시 테스트구");
        order.setPhone("010-3333-4444");
        order.addOrderItem(new OrderItem(product, 1, 20_000L, "FREE"));
        order.calculateTotalPrice();
        return orderRepository.save(order);
    }

    private Review saveReview(User author, String content) {
        Order order = saveDeliveredOrder(author);
        return reviewRepository.save(new Review(author, product, order, 5, content, List.of()));
    }

    private String reviewJson(Long claimedUserId, Order order, String content) {
        return "{\"userId\":%d,\"productId\":%d,\"orderId\":%d,\"rating\":5,\"content\":\"%s\",\"imageUrls\":[]}"
                .formatted(claimedUserId, product.getId(), order.getId(), content);
    }

    private Review findByContent(String content) {
        return reviewRepository.findAll().stream()
                .filter(review -> content.equals(review.getContent()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("리뷰가 저장되지 않았습니다: " + content));
    }
}
