package com.spendly.controller;

import com.spendly.dto.LendingRequest;
import com.spendly.dto.RepaymentRequest;
import com.spendly.entity.LendingObligation;
import com.spendly.service.LendingObligationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/obligations", "/api/lending"})
public class LendingObligationController {

    private final LendingObligationService obligationService;

    public LendingObligationController(LendingObligationService obligationService) {
        this.obligationService = obligationService;
    }

    @GetMapping
    public ResponseEntity<List<LendingObligation>> getAll() {
        return ResponseEntity.ok(obligationService.getAllObligations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LendingObligation> getById(@PathVariable Long id) {
        return obligationService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LendingObligation> create(@Valid @RequestBody LendingRequest request) {
        return ResponseEntity.ok(obligationService.createObligation(request));
    }

    @PostMapping("/{id}/repayments")
    public ResponseEntity<LendingObligation> addRepayment(@PathVariable Long id, @Valid @RequestBody RepaymentRequest request) {
        return ResponseEntity.ok(obligationService.recordRepayment(id, request));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<LendingObligation> settle(@PathVariable Long id) {
        return ResponseEntity.ok(obligationService.markSettled(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        obligationService.deleteObligation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(obligationService.getSummary());
    }
}
