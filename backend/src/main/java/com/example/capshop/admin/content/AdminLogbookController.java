package com.example.capshop.admin.content;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.content.Logbook;
import com.example.capshop.repository.content.LogbookRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/logbook")
public class AdminLogbookController {

    private final LogbookRepository logbookRepository;

    @PostMapping
    public Logbook createLogbook(@RequestBody Logbook req) {
        Integer maxOrder = logbookRepository.findMaxSortOrder();
        int nextOrder = (maxOrder == null ? 0 : maxOrder + 1);

        Logbook saved = Logbook.builder()
                .imageUrl(req.getImageUrl())
                .sortOrder(nextOrder)
                .build();

        return logbookRepository.save(saved);
    }

    @DeleteMapping("/{id}")
    public void deleteLogbook(@PathVariable("id") Long id) {
        logbookRepository.deleteById(id);
    }
}
