package com.example.capshop.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.capshop.domain.Product;
import com.example.capshop.domain.CartItem;
import com.example.capshop.domain.User;
import com.example.capshop.dto.CartItemResponse;
import com.example.capshop.repository.CartItemRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CartItemService {
    private final CartItemRepository cartItemRepository;

    public CartItem addToCart(User user, Product product, int quantity, String size) {
        // 같은 사용자, 같은 상품, 같은 사이즈인 아이템이 있는지 확인
        Optional<CartItem> existingItem = cartItemRepository.findByUserAndProductAndSize(user, product, size);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            return cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(user, product, quantity, size);
            return cartItemRepository.save(newItem);
        }
    }
    
    // 기존 메서드 유지 (하위 호환성)
    public CartItem addToCart(User user, Product product, int quantity) {
        return addToCart(user, product, quantity, null);
    }

    public void deleteCartItem(User user, Product product, String size) {
        // deleteCartItem invoked
        // 먼저 정확한 사이즈로 찾기
        Optional<CartItem> existingItem = cartItemRepository.findByUserAndProductAndSize(user, product, size);
        
        // 찾지 못했고 size가 null이 아닌 경우, null 사이즈로 저장된 아이템 찾기
        if (existingItem.isEmpty() && size != null) {
            // not found with exact size; retry with null size
            existingItem = cartItemRepository.findByUserAndProductAndSize(user, product, null);
        }
        
        if (existingItem.isPresent()) {
            cartItemRepository.delete(existingItem.get());
        } else {
            // item not found; throwing
            // debugging listing suppressed
            throw new IllegalArgumentException("장바구니에 해당 상품이 없습니다. (사이즈: '" + size + "')");
        }
    }
    
    // 기존 메서드 유지 (하위 호환성) - 사이즈 없이 삭제하는 경우 첫 번째 발견된 아이템 삭제
    public void deleteCartItem(User user, Product product) {
        List<CartItem> items = cartItemRepository.findAllByUserAndProduct(user, product);
        if (!items.isEmpty()) {
            cartItemRepository.delete(items.get(0));
        } else {
            throw new IllegalArgumentException("장바구니에 해당 상품이 없습니다.");
        }
    }
    @Transactional
    public int increaseQuantity(User user, Product product, String size) {
        // increaseQuantity invoked
        // 먼저 정확한 사이즈로 찾기
        Optional<CartItem> itemOpt = cartItemRepository.findByUserAndProductAndSize(user, product, size);
        
        // 찾지 못했고 size가 null이 아닌 경우, null 사이즈로 저장된 아이템 찾기
        if (itemOpt.isEmpty() && size != null) {
            // not found with exact size; retry with null size
            itemOpt = cartItemRepository.findByUserAndProductAndSize(user, product, null);
            
            // null 사이즈 아이템을 찾았다면 사이즈를 업데이트
            if (itemOpt.isPresent()) {
                CartItem existingItem = itemOpt.get();
                existingItem.setSize(size);
                cartItemRepository.save(existingItem);
                // updated existing null-size item with new size
            }
        }
        
        // 장바구니에서 해당 유저 + 상품 + 사이즈 찾기
        CartItem item = itemOpt.orElseGet(() -> {
            // creating new CartItem since none exists
            CartItem ci = new CartItem();
            ci.setUser(user);
            ci.setProduct(product);
            ci.setSize(size);
            ci.setQuantity(0);
            return ci;
        });

        int next = item.getQuantity() + 1;

        // 사이즈별 재고 체크
        Long stockBySize = product.getStockBySize(size);
        if (stockBySize != null && next > stockBySize) {
            throw new IllegalStateException("재고를 초과했습니다. (사이즈 " + size + " 재고: " + stockBySize + "개)");
        }
        
        // 전체 재고 체크 (하위 호환성)
        if (product.getStock() != null && next > product.getStock()) {
            throw new IllegalStateException("재고를 초과했습니다.");
        }

        item.setQuantity(next);
        cartItemRepository.save(item);
        // item quantity incremented
        return next;
    }
    
    // 기존 메서드 유지 (하위 호환성)
    @Transactional
    public int increaseQuantity(User user, Product product) {
        // 사이즈 없이 호출된 경우 첫 번째 발견된 아이템의 수량 증가
        List<CartItem> items = cartItemRepository.findAllByUserAndProduct(user, product);
        CartItem item;
        
        if (items.isEmpty()) {
            // 아이템이 없으면 새로 생성 (사이즈 null)
            item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setQuantity(0);
        } else {
            item = items.get(0);
        }

        int next = item.getQuantity() + 1;

        // (선택) 재고 체크
        if (product.getStock() != null && next > product.getStock()) {
            throw new IllegalStateException("재고를 초과했습니다.");
        }

        item.setQuantity(next);
        cartItemRepository.save(item);
        return next;
    }

    @Transactional
    public int decreaseQuantity(User user, Product product, String size) {
        // decreaseQuantity invoked
        
        // 먼저 정확한 사이즈로 찾기
        Optional<CartItem> itemOpt = cartItemRepository.findByUserAndProductAndSize(user, product, size);
        
        // 찾지 못했고 size가 null이 아닌 경우, null 사이즈로 저장된 아이템 찾기
        if (itemOpt.isEmpty() && size != null) {
            // not found with exact size; retry with null size
            itemOpt = cartItemRepository.findByUserAndProductAndSize(user, product, null);
        }
        
        CartItem item = itemOpt.orElseThrow(() -> 
            new IllegalArgumentException("장바구니에 해당 상품이 없습니다. (사이즈: '" + size + "')"));

        int current = item.getQuantity();
        if (current > 1) {
            item.setQuantity(current - 1);
            cartItemRepository.save(item);
            return item.getQuantity();
        } else {
            // current == 1 → 더 이상 감소하지 않음
            return 1;
        }
    }

    // 기존 메서드 유지 (하위 호환성)
    @Transactional
    public int decreaseQuantity(User user, Product product) {
        List<CartItem> items = cartItemRepository.findAllByUserAndProduct(user, product);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("장바구니에 해당 상품이 없습니다.");
        }
        
        CartItem item = items.get(0);
        int current = item.getQuantity();
        if (current > 1) {
            item.setQuantity(current - 1);
            cartItemRepository.save(item);
            return item.getQuantity();
        } else {
            // current == 1 → 더 이상 감소하지 않음
            return 1;
        }
    }


    public List<CartItemResponse> allCartItemResponse(User user) {
        List<CartItem> items = cartItemRepository.findByUser(user);
        return items.stream()
            .map(item -> new CartItemResponse(
                item.getId(),
                item.getQuantity(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice(),
                item.getProduct().getMainImageUrl(),
                item.getSize()
            ))
            .collect(Collectors.toList());
    }
    
    // 특정 사용자, 상품, 사이즈의 장바구니 수량 조회
    public int getCartItemQuantity(User user, Product product, String size) {
        // size가 빈 문자열이거나 null인 경우 null로 통일
        if (size != null && size.trim().isEmpty()) {
            size = null;
        }
        
        // 먼저 정확한 사이즈로 찾기
        Optional<CartItem> itemOpt = cartItemRepository.findByUserAndProductAndSize(user, product, size);
        
        // 찾지 못했고 size가 null이 아닌 경우, null 사이즈로 저장된 아이템 찾기
        if (itemOpt.isEmpty() && size != null) {
            itemOpt = cartItemRepository.findByUserAndProductAndSize(user, product, null);
        }
        
        return itemOpt.map(CartItem::getQuantity).orElse(0);
    }
}
