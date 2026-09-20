package com.example.capshop.controller.content;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capshop.domain.content.Logbook;
import com.example.capshop.repository.content.LogbookRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class LogbookController {

    private final LogbookRepository logbookRepository;

    @GetMapping("/api/logbook")
    public List<Logbook> getLogbook() {
        return logbookRepository.findAllByOrderByIdDesc();
    }
}
