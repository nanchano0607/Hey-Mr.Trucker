package com.example.capshop.controller.cart;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.product.Product;
import com.example.capshop.domain.user.User;
import com.example.capshop.dto.cart.AddCartItemRequest;
import com.example.capshop.service.cart.CartItemService;
import com.example.capshop.service.product.ProductService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartItemController {
    private final ProductService productService;
    private final CartItemService cartItemService;
    @PostMapping("/save")
    public void cartIn(@AuthenticationPrincipal User user, @RequestBody AddCartItemRequest request){
        
        Product product = productService.findById(request.getProductId());
        int quantity = request.getQuantity();
        String size = request.getSize();
        cartItemService.addToCart(user, product, quantity, size);
    }
    @PostMapping("/increase")
    public ResponseEntity<Integer> increase(@AuthenticationPrincipal User user, @RequestBody AddCartItemRequest request) {
        Product product = productService.findById(request.getProductId());
        String size = request.getSize();
        int qty   = cartItemService.increaseQuantity(user, product, size);
        return ResponseEntity.ok(qty);
    }

    // - 버튼: quantity 만큼 감소 (0 이하되면 삭제하고 0 반환)
    @PostMapping("/decrease")
    public ResponseEntity<Integer> decrease(@AuthenticationPrincipal User user, @RequestBody AddCartItemRequest request) {
        Product product = productService.findById(request.getProductId());
        String size = request.getSize();
        int qty   = cartItemService.decreaseQuantity(user, product, size);
        return ResponseEntity.ok(qty);
    }

    // 휴지통: 해당 아이템 전체 삭제 (quantity는 무시)
    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @RequestBody AddCartItemRequest request) {
        Product product = productService.findById(request.getProductId());
        String size = request.getSize();
        cartItemService.deleteCartItem(user, product, size);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/findAll")
        public List<com.example.capshop.dto.cart.CartItemResponse> findAllCartItem(@AuthenticationPrincipal User user) {
            return cartItemService.allCartItemResponse(user);
    }
    
    // 특정 상품의 특정 사이즈가 장바구니에 몇 개 담겨있는지 확인
    @GetMapping("/find")
    public ResponseEntity<Integer> findCartItemQuantity(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "productId") Long productId,
            @RequestParam(name = "size") String size) {
        Product product = productService.findById(productId);
        
        int quantity = cartItemService.getCartItemQuantity(user, product, size);
        return ResponseEntity.ok(quantity);
    }

}
