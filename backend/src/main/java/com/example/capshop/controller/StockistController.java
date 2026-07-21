package com.example.capshop.controller;

import java.util.List;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.Stockist;
import com.example.capshop.repository.StockistRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StockistController {

    private final StockistRepository stockistRepository;

    @GetMapping("/api/stockist")
    public List<Stockist> getStockists() {
        return stockistRepository.findAllByOrderByIdDesc();
    }

    @PostMapping("/api/stockist")
    public Stockist createStockist(@RequestBody Stockist request) {
        String imageUrl = StringUtils.hasText(request.getImageUrl())
                ? request.getImageUrl().trim()
                : "";

        Stockist stockist = Stockist.builder()
                .imageUrl(imageUrl)
                .build();

        return stockistRepository.save(stockist);
    }

    @DeleteMapping("/api/stockist/{id}")
    public void deleteStockist(@PathVariable("id") Long id) {
        stockistRepository.deleteById(id);
    }
}
