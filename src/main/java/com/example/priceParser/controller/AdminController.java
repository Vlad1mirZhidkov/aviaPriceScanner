package com.example.priceParser.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import com.example.priceParser.service.DataParsingService;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@Slf4j
@AllArgsConstructor
public class AdminController {
    private final DataParsingService parsingService;

    @PostMapping("/parseAirports")
    public ResponseEntity<?> parseData() {
        try {
            parsingService.parseAllData();
            return ResponseEntity.ok("Данные успешно обновлены");
        } catch (Exception e) {
            log.error("Ошибка при парсинге", e);
            return ResponseEntity.internalServerError()
                .body("Ошибка: " + e.getMessage());
        }
    }
} 