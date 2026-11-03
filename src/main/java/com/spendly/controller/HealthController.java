package com.spendly.controller;

import com.spendly.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final TransactionRepository transactionRepository;

    public HealthController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @RequestMapping(
        value = {"/api/health", "/api/health/", "/health", "/health/", "/healthz", "/healthz/", "/ping", "/ping/"},
        method = {RequestMethod.GET, RequestMethod.HEAD}
    )
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "UP");
        map.put("service", "Spendly Personal Finance Backend");
        map.put("timestamp", LocalDateTime.now());
        try {
            map.put("totalTransactions", transactionRepository.count());
        } catch (Exception ignored) {
            map.put("totalTransactions", 0);
        }
        return ResponseEntity.ok(map);
    }
}
