package com.spendly.controller;

import com.spendly.dto.ImportSummaryResponse;
import com.spendly.entity.StatementImport;
import com.spendly.service.StatementImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/statements")
public class StatementImportController {

    private final StatementImportService importService;

    public StatementImportController(StatementImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportSummaryResponse> importStatement(@RequestParam("file") MultipartFile file) {
        ImportSummaryResponse response = importService.importStatement(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"", "/history"})
    public ResponseEntity<List<StatementImport>> getImports() {
        return ResponseEntity.ok(importService.getAllImports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatementImport> getImportById(@PathVariable Long id) {
        return ResponseEntity.ok(importService.getImportById(id));
    }
}
