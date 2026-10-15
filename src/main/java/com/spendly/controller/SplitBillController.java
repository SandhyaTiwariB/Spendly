package com.spendly.controller;

import com.spendly.dto.SplitBillRequest;
import com.spendly.entity.SplitBill;
import com.spendly.service.SplitBillService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/split-bills", "/api/splits"})
public class SplitBillController {

    private final SplitBillService splitBillService;

    public SplitBillController(SplitBillService splitBillService) {
        this.splitBillService = splitBillService;
    }

    @GetMapping
    public ResponseEntity<List<SplitBill>> getAll() {
        return ResponseEntity.ok(splitBillService.getAllBills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SplitBill> getById(@PathVariable Long id) {
        return splitBillService.getBillById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SplitBill> create(@Valid @RequestBody SplitBillRequest request) {
        return ResponseEntity.ok(splitBillService.createBill(request));
    }

    @PostMapping("/{billId}/settle/{participantId}")
    public ResponseEntity<SplitBill> settle(@PathVariable Long billId, @PathVariable Long participantId) {
        return ResponseEntity.ok(splitBillService.settleParticipant(billId, participantId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        splitBillService.deleteBill(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/net-balance")
    public ResponseEntity<Map<String, Object>> getNetBalance() {
        return ResponseEntity.ok(splitBillService.calculateNetBalance());
    }
}
