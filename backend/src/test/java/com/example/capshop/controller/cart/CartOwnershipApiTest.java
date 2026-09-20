package com.example.capshop.controller.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.product.ProductStock;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.product.ProductRepository;
import com.example.capshop.repository.product.ProductStockRepository;
import com.example.capshop.support.ApiTestSupport;

/** 장바구니는 요청에 담긴 userId 를 무시하고 항상 로그인한 사용자의 장바구니만 다룬다. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CartOwnershipApiTest extends ApiTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductStockRepository productStockRepository;

    private User alice;
    private User bob;
    private String aliceToken;
    private String bobToken;
    private Product product;

    @BeforeAll
    void setUp() {
        alice = saveUser("cart-alice", false);
        bob = saveUser("cart-bob", false);
        aliceToken = bearer(alice);
        bobToken = bearer(bob);

        Product newProduct = new Product();
        newProduct.setName("장바구니 소유권 테스트 캡 " + System.nanoTime());
        newProduct.setPrice(20_000L);
        product = productRepository.save(newProduct);
        productStockRepository.save(new ProductStock(product, "FREE", 100L));
    }

    @Test
    @DisplayName("다른 사용자의 userId 를 담아 장바구니에 넣어도 로그인한 본인의 장바구니에만 담긴다")
    void save_ignoresUserIdInBody() throws Exception {
        // Arrange
        User owner = saveUser("cart-save-owner", false);
        User victim = saveUser("cart-save-victim", false);

        // Act - owner 가 victim 의 userId 를 body 에 넣어 담기
        mockMvc.perform(post("/api/cart/save")
                .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":%d,\"productId\":%d,\"quantity\":2,\"size\":\"FREE\"}"
                        .formatted(victim.getId(), product.getId())))
                .andExpect(status().isOk());

        // Assert
        mockMvc.perform(get("/api/cart/findAll").header(HttpHeaders.AUTHORIZATION, bearer(owner)).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quantity").value(2));
        mockMvc.perform(get("/api/cart/findAll").header(HttpHeaders.AUTHORIZATION, bearer(victim)).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("장바구니 조회는 userId 쿼리 파라미터를 무시하고 본인 장바구니만 돌려준다")
    void findAll_ignoresUserIdParam() throws Exception {
        // Arrange - alice 만 담아 둔다
        User owner = saveUser("cart-find-owner", false);
        addToCart(bearer(owner), 3);

        // Act & Assert - bob 이 owner 의 userId 를 파라미터로 넣어도 bob 의 (빈) 장바구니만 보인다
        mockMvc.perform(get("/api/cart/findAll").param("userId", String.valueOf(owner.getId()))
                .header(HttpHeaders.AUTHORIZATION, bobToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("담긴 수량 조회는 본인 장바구니 기준이다")
    void find_usesLoggedInUsersCart() throws Exception {
        // Arrange
        User owner = saveUser("cart-qty-owner", false);
        addToCart(bearer(owner), 4);

        // Act & Assert
        mockMvc.perform(get("/api/cart/find").param("productId", String.valueOf(product.getId())).param("size", "FREE")
                .header(HttpHeaders.AUTHORIZATION, bearer(owner)).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(content().string("4"));
        mockMvc.perform(get("/api/cart/find").param("userId", String.valueOf(owner.getId()))
                .param("productId", String.valueOf(product.getId())).param("size", "FREE")
                .header(HttpHeaders.AUTHORIZATION, bobToken).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    @DisplayName("수량 증가·감소·삭제는 본문에 다른 사용자의 userId 가 있어도 본인 장바구니에만 적용된다")
    void increaseDecreaseDelete_affectOnlyLoggedInUsersCart() throws Exception {
        // Arrange
        User owner = saveUser("cart-mut-owner", false);
        addToCart(bearer(owner), 2);
        String forgedBody = "{\"userId\":%d,\"productId\":%d,\"size\":\"FREE\"}".formatted(owner.getId(), product.getId());

        // Act - bob 이 owner 의 userId 를 넣어 증가/감소/삭제 시도
        for (String action : new String[] {"increase", "decrease", "delete"}) {
            mockMvc.perform(post("/api/cart/" + action)
                    .header(HttpHeaders.AUTHORIZATION, bobToken)
                    .with(asDefaultServlet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(forgedBody));
        }

        // Assert - owner 의 장바구니는 그대로 (수량 2)
        mockMvc.perform(get("/api/cart/findAll").header(HttpHeaders.AUTHORIZATION, bearer(owner)).with(asDefaultServlet()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quantity").value(2));
        assertThat(bob.getId()).isNotEqualTo(owner.getId());
    }

    private void addToCart(String token, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/save")
                .header(HttpHeaders.AUTHORIZATION, token)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":%d,\"quantity\":%d,\"size\":\"FREE\"}".formatted(product.getId(), quantity)))
                .andExpect(status().isOk());
    }
}
