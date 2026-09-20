package com.example.capshop.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.capshop.domain.content.Notice;
import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.product.ProductStock;
import com.example.capshop.domain.product.ProductType;
import com.example.capshop.domain.product.VintageCategory;
import com.example.capshop.domain.user.User;
import com.example.capshop.repository.content.NoticeRepository;
import com.example.capshop.repository.product.ProductRepository;
import com.example.capshop.repository.product.ProductStockRepository;
import com.example.capshop.support.ApiTestSupport;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminEndpointBehaviorTest extends ApiTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductStockRepository productStockRepository;
    @Autowired
    private NoticeRepository noticeRepository;

    private User admin;
    private String adminToken;
    private String userToken;

    @BeforeAll
    void setUpAccounts() {
        admin = saveUser("behavior-admin", true);
        adminToken = bearer(admin);
        userToken = bearer(saveUser("behavior-user", false));
    }

    @Test
    @DisplayName("관리자가 /api/admin/cap/save 로 캡 상품을 등록하면 CAP 타입과 사이즈별 재고가 저장된다")
    void saveCap_storesProductWithTypeAndStock() throws Exception {
        // Arrange
        String name = "admin-cap-" + System.nanoTime();
        String body = """
                {"name":"%s","price":25000,"color":"black","sizeInfo":"info",
                 "mainImageUrl":"/uploads/cap/main.png","size":["FREE"],
                 "imageUrls":["/uploads/cap/a.png"],"sizeStocks":{"FREE":5}}
                """.formatted(name);

        // Act
        mockMvc.perform(post("/api/admin/cap/save")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        // Assert
        Product saved = productRepository.findByNameContaining(name).get(0);
        assertThat(saved.getProductType()).isEqualTo(ProductType.CAP);
        assertThat(saved.getPrice()).isEqualTo(25000L);
        assertThat(productStockRepository.findByProductAndSize(saved, "FREE").orElseThrow().getStock())
                .isEqualTo(5L);
    }

    @Test
    @DisplayName("관리자가 /api/admin/vintage/save 로 빈티지 상품을 등록하면 VINTAGE 타입과 카테고리가 저장된다")
    void saveVintage_storesVintageCategory() throws Exception {
        // Arrange
        String name = "admin-vintage-" + System.nanoTime();
        String body = """
                {"name":"%s","price":80000,"color":"navy","sizeInfo":"info",
                 "mainImageUrl":"/uploads/cap/main.png","size":["M"],
                 "imageUrls":[],"vintageCategory":"shirt"}
                """.formatted(name);

        // Act
        mockMvc.perform(post("/api/admin/vintage/save")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        // Assert
        Product saved = productRepository.findByNameContaining(name).get(0);
        assertThat(saved.getProductType()).isEqualTo(ProductType.VINTAGE);
        assertThat(saved.getVintageCategory()).isEqualTo(VintageCategory.SHIRT);
    }

    @Test
    @DisplayName("관리자가 사이즈별 재고 수정 API를 호출하면 해당 사이즈 재고가 갱신된다")
    void updateStockBySize_updatesStock() throws Exception {
        // Arrange
        Product product = savedProduct("admin-stock-" + System.nanoTime());
        productStockRepository.save(new ProductStock(product, "FREE", 1L));

        // Act
        mockMvc.perform(post("/api/admin/acc/updateStock/{id}/{size}", product.getId(), "FREE")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("7"))
                .andExpect(status().isOk());

        // Assert
        assertThat(productStockRepository.findByProductAndSize(product, "FREE").orElseThrow().getStock())
                .isEqualTo(7L);
    }

    @Test
    @DisplayName("관리자가 신상품 지정/해제 API를 호출하면 isNew 값이 토글된다")
    void setNewAndUnsetNew_togglesIsNew() throws Exception {
        // Arrange
        Product product = savedProduct("admin-new-" + System.nanoTime());

        // Act
        mockMvc.perform(post("/api/admin/product/setNew/{id}", product.getId())
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet()))
                .andExpect(status().isOk());
        Boolean afterSet = productRepository.findById(product.getId()).orElseThrow().getIsNew();

        mockMvc.perform(post("/api/admin/cap/unsetNew/{id}", product.getId())
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet()))
                .andExpect(status().isOk());
        Boolean afterUnset = productRepository.findById(product.getId()).orElseThrow().getIsNew();

        // Assert
        assertThat(afterSet).isTrue();
        assertThat(afterUnset).isFalse();
    }

    @Test
    @DisplayName("관리자가 가격을 수정하면 이전 가격이 pastPrice에 보관된다")
    void updatePrice_keepsPastPrice() throws Exception {
        // Arrange
        Product product = savedProduct("admin-price-" + System.nanoTime());

        // Act
        mockMvc.perform(put("/api/admin/product/{id}/price", product.getId())
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("20000"))
                .andExpect(status().isOk());

        // Assert
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getPrice()).isEqualTo(20000L);
        assertThat(updated.getPastPrice()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("관리자가 공지를 작성/수정/삭제하면 작성자는 로그인한 관리자로 기록된다")
    void notice_adminCreatesUpdatesAndDeletes() throws Exception {
        // Arrange
        String title = "admin-notice-" + System.nanoTime();

        // Act & Assert - 작성
        mockMvc.perform(post("/api/admin/notices")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"%s\",\"content\":\"본문\"}".formatted(title)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value(admin.getName()));
        Notice created = findNoticeByTitle(title);

        // Act & Assert - 수정
        mockMvc.perform(put("/api/admin/notices/{id}", created.getId())
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"%s-edited\"}".formatted(title)))
                .andExpect(status().isOk());
        assertThat(noticeRepository.findById(created.getId()).orElseThrow().getTitle()).isEqualTo(title + "-edited");

        // Act & Assert - 삭제
        mockMvc.perform(delete("/api/admin/notices/{id}", created.getId())
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet()))
                .andExpect(status().isOk());
        assertThat(noticeRepository.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("일반 사용자가 관리자 userId를 파라미터로 넘겨도 공지를 작성할 수 없다")
    void notice_normalUserCannotImpersonateAdminWithUserIdParam() throws Exception {
        // Arrange
        String title = "forged-notice-" + System.nanoTime();

        // Act
        mockMvc.perform(post("/api/admin/notices")
                .param("userId", String.valueOf(admin.getId()))
                .header(HttpHeaders.AUTHORIZATION, userToken)
                .with(asDefaultServlet())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"%s\",\"content\":\"위조\"}".formatted(title)))
                .andExpect(status().isForbidden());

        // Assert
        assertThat(noticeRepository.findAll()).extracting(Notice::getTitle).doesNotContain(title);
    }

    @Test
    @DisplayName("관리자가 로고를 변경하면 공개 GET /api/logo 응답에 반영된다")
    void logo_adminChangeIsVisibleFromPublicEndpoint() throws Exception {
        // Arrange
        String filename = "logo-" + System.nanoTime() + ".png";

        try {
            // Act
            mockMvc.perform(post("/api/admin/logo")
                    .param("filename", filename)
                    .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet()))
                    .andExpect(status().isOk());

            // Assert
            mockMvc.perform(get("/api/logo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.endsWith("/" + filename)));
        } finally {
            mockMvc.perform(post("/api/admin/logo")
                    .param("filename", "homelogo.webp")
                    .header(HttpHeaders.AUTHORIZATION, adminToken)
                .with(asDefaultServlet()));
        }
    }

    private Product savedProduct(String name) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(10000L);
        return productRepository.save(product);
    }

    private Notice findNoticeByTitle(String title) {
        List<Notice> matched = noticeRepository.findAll().stream()
                .filter(n -> title.equals(n.getTitle()))
                .toList();
        assertThat(matched).hasSize(1);
        return matched.get(0);
    }
}
