package com.example.capshop.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.Logbook;
import com.example.capshop.repository.LogbookRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class LogbookController {

    private final LogbookRepository logbookRepository;

    @GetMapping("/api/logbook")
    public List<Logbook> getLogbook() {
        return logbookRepository.findAllByOrderByIdDesc();
    }

    @PostMapping("/api/logbook")
    public Logbook createLogbook(@RequestBody Logbook req) {
        // sortOrder 자동 부여 예시
        Integer maxOrder = logbookRepository.findMaxSortOrder();
        int nextOrder = (maxOrder == null ? 0 : maxOrder + 1);

        Logbook saved = Logbook.builder()
                .imageUrl(req.getImageUrl())
                .sortOrder(nextOrder)
                .build();

        return logbookRepository.save(saved);
    }

    @DeleteMapping("/api/logbook/{id}")
    public void deleteLogbook(@PathVariable("id")Long id) {
        logbookRepository.deleteById(id);
    }
}
