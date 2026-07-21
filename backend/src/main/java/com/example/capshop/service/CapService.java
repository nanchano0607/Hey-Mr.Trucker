package com.example.capshop.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.capshop.domain.Cap;
import com.example.capshop.repository.CapRepository;
import com.example.capshop.repository.CapStockRepository;
import com.example.capshop.repository.CartItemRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CapService {
    private final CartItemRepository cartItemRepository;
    private final CapRepository capRepository;
    private final CapStockRepository capStockRepository;
    public Cap save(Cap cap){
        return capRepository.save(cap);
    }

    public List<Cap> findAll() {
        return capRepository.findAll();
    }
    
    @Transactional
    public void deleteById(Long id) {
        cartItemRepository.deleteByProductId(id);
        capRepository.deleteById(id);
    }

    public List<Cap> findByName(String keyword) {
        return capRepository.findByNameContaining(keyword);
    }
    public Cap findById(Long id) {
    return capRepository.findById(id).orElse(null); // 없으면 null
    }
    
    public List<Cap> findNewCaps() {
        return capRepository.findByIsNewTrue();
    }
    
    public void setIsNew(Long id, boolean isNew) {
        Cap cap = capRepository.findById(id).orElse(null);
        if (cap != null) {
            cap.setIsNew(isNew);
            capRepository.save(cap);
        }
    }
    
    // 사이즈별 재고 업데이트
    public void updateStockBySize(Long capId, String size, Long stock) {
        Cap cap = capRepository.findById(capId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + capId));
        
        // 해당 사이즈의 CapStock 찾기 또는 생성
        com.example.capshop.domain.CapStock capStock = cap.getCapStockBySize(size);
        if (capStock == null) {
            // 새로운 사이즈 재고 생성
            capStock = new com.example.capshop.domain.CapStock(cap, size, stock);
            if (cap.getStocks() == null) {
                cap.setStocks(new java.util.ArrayList<>());
            }
            cap.getStocks().add(capStock);
        } else {
            capStock.setStock(stock);
        }
        
        capRepository.save(cap);
    }
    
    // 기존 메서드 유지 (하위 호환성)
    public void updateStock(Long id, Long stock) {
        Cap cap = capRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + id));
        cap.setStock(stock);
        capRepository.save(cap);
    }
}

