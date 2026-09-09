package com.example.capshop.service;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.capshop.domain.CartItem;
import com.example.capshop.domain.Product;
import com.example.capshop.domain.ProductStock;
import com.example.capshop.domain.User;
import com.example.capshop.repository.CartItemRepository;
import com.example.capshop.repository.ProductRepository;
import com.example.capshop.repository.ProductStockRepository;
import com.example.capshop.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceStockConcurrencyTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductStockRepository productStockRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    @DisplayName("재고 1개인 상품을 두 명이 동시에 주문하면 한 명만 성공하고 재고는 0으로 남는다")
    void concurrentPlaceOrder_preventsOversell() throws InterruptedException {
        // Arrange
        Product product = productRepository.save(newProduct("동시성 테스트 캡"));
        productStockRepository.save(new ProductStock(product, "FREE", 1L));

        User userA = userRepository.save(newUser("concurrency-a@test.com", "010-0000-1111"));
        User userB = userRepository.save(newUser("concurrency-b@test.com", "010-0000-2222"));

        cartItemRepository.save(new CartItem(userA, product, 1, "FREE"));
        cartItemRepository.save(new CartItem(userB, product, 1, "FREE"));

        List<User> orderers = List.of(userA, userB);
        ExecutorService executor = Executors.newFixedThreadPool(orderers.size());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(orderers.size());
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (User orderer : orderers) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    orderService.placeOrder(orderer);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Act: 두 스레드가 최대한 동시에 placeOrder를 호출하도록 한 번에 출발시킨다.
        startLatch.countDown();
        boolean finishedInTime = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertThat(finishedInTime).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        ProductStock finalStock = productStockRepository.findByProductAndSize(product, "FREE").orElseThrow();
        assertThat(finalStock.getStock()).isEqualTo(0L);
    }

    private Product newProduct(String name) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(10000L);
        return product;
    }

    private User newUser(String email, String phone) {
        return User.builder()
                .email(email)
                .password("test-password")
                .name("테스트유저")
                .phone(phone)
                .build();
    }
}
