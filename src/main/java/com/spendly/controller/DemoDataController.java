package com.spendly.controller;

import com.spendly.dto.DemoDataGenerateRequest;
import com.spendly.dto.DemoDataResponse;
import com.spendly.service.DemoDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo")
public class DemoDataController {

    private final DemoDataService demoDataService;

    public DemoDataController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/generate")
    public ResponseEntity<DemoDataResponse> generate(@RequestBody DemoDataGenerateRequest request) {
        return ResponseEntity.ok(demoDataService.generateDemoData(request));
    }

    @PostMapping("/load-sample")
    public ResponseEntity<DemoDataResponse> loadSample() {
        return ResponseEntity.ok(demoDataService.loadStandardSampleDataset());
    }

    @PostMapping("/reset")
    public ResponseEntity<DemoDataResponse> reset() {
        return ResponseEntity.ok(demoDataService.resetDemoData());
    }

    @GetMapping("/status")
    public ResponseEntity<DemoDataResponse> getStatus() {
        return ResponseEntity.ok(demoDataService.getDemoStatus());
    }
}
