package com.example.capshop.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.Cap;
import com.example.capshop.domain.Product;
import com.example.capshop.domain.User;
import com.example.capshop.dto.AddCartItemRequest;
import com.example.capshop.service.CapService;
import com.example.capshop.service.CartItemService;
import com.example.capshop.service.ProductService;
import com.example.capshop.service.UserService;

import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartItemController {
    private final UserService userService;
    private final ProductService productService;
    private final CartItemService cartItemService;
    @PostMapping("/save")
    public void cartIn(@RequestBody AddCartItemRequest request){
        
        User user = userService.findById(request.getUserId());
        Product product = productService.findById(request.getProductId());
        int quantity = request.getQuantity();
        String size = request.getSize();
        cartItemService.addToCart(user, product, quantity, size);
    }
    @PostMapping("/increase")
    public ResponseEntity<Integer> increase(@RequestBody AddCartItemRequest request) {
        User user = userService.findById(request.getUserId());
        Product product = productService.findById(request.getProductId());
        String size = request.getSize();
        int qty   = cartItemService.increaseQuantity(user, product, size);
        return ResponseEntity.ok(qty);
    }

    // - 버튼: quantity 만큼 감소 (0 이하되면 삭제하고 0 반환)
    @PostMapping("/decrease")
    public ResponseEntity<Integer> decrease(@RequestBody AddCartItemRequest request) {
        User user = userService.findById(request.getUserId());
        Product product = productService.findById(request.getProductId());
        String size = request.getSize();
        int qty   = cartItemService.decreaseQuantity(user, product, size);
        return ResponseEntity.ok(qty);
    }

    // 휴지통: 해당 아이템 전체 삭제 (quantity는 무시)
    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody AddCartItemRequest request) {
        User user = userService.findById(request.getUserId());
        Product product = productService.findById(request.getProductId());
        String size = request.getSize();
        cartItemService.deleteCartItem(user, product, size);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/findAll")
        public List<com.example.capshop.dto.CartItemResponse> findAllCartItem(@RequestParam("userId") Long userId) {
            User user = userService.findById(userId);
            return cartItemService.allCartItemResponse(user);
    }
    
    // 특정 상품의 특정 사이즈가 장바구니에 몇 개 담겨있는지 확인
    @GetMapping("/find")
    public ResponseEntity<Integer> findCartItemQuantity(
            @RequestParam(name = "userId") Long userId, 
            @RequestParam(name = "productId") Long productId,
            @RequestParam(name = "size") String size) {
        User user = userService.findById(userId);
        Product product = productService.findById(productId);
        
        int quantity = cartItemService.getCartItemQuantity(user, product, size);
        return ResponseEntity.ok(quantity);
    }

}
