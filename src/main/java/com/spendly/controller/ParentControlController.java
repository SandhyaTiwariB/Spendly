package com.spendly.controller;

import com.spendly.dto.ParentRuleUpdateRequest;
import com.spendly.dto.ParentSummaryResponse;
import com.spendly.entity.ParentActivityLog;
import com.spendly.entity.ParentAlert;
import com.spendly.entity.ParentRule;
import com.spendly.service.ParentControlService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parent-control")
public class ParentControlController {

    private final ParentControlService parentControlService;
    private final com.spendly.service.AuthService authService;

    public ParentControlController(ParentControlService parentControlService, com.spendly.service.AuthService authService) {
        this.parentControlService = parentControlService;
        this.authService = authService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ParentSummaryResponse> getSummary(
            @RequestParam(value = "childAccount", defaultValue = "Aarav's account") String childAccount) {
        return ResponseEntity.ok(parentControlService.getSummary(childAccount));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<ParentRule>> getRules(
            @RequestParam(value = "childAccount", defaultValue = "Aarav's account") String childAccount) {
        return ResponseEntity.ok(parentControlService.getRules(childAccount));
    }

    @PutMapping("/rules/{category}")
    public ResponseEntity<?> updateRule(
            @PathVariable String category,
            @RequestParam(value = "childAccount", defaultValue = "Aarav's account") String childAccount,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ParentRuleUpdateRequest request) {

        com.spendly.entity.User user = authService.getUserByToken(authHeader);
        if (user != null && "CHILD".equalsIgnoreCase(user.getRole())) {
            parentControlService.recordBlockedTransaction(
                    childAccount,
                    "Unauthorized rule modification attempt for '" + category + "'",
                    java.math.BigDecimal.ZERO,
                    category,
                    "Security Violation: Child accounts cannot modify parental restrictions"
            );
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access Denied: Minor/child accounts are not permitted to modify parental rules."));
        }

        return ResponseEntity.ok(parentControlService.updateRule(childAccount, category, request));
    }

    @GetMapping("/activity-log")
    public ResponseEntity<Page<ParentActivityLog>> getActivityLog(
            @RequestParam(value = "childAccount", required = false) String childAccount,
            @RequestParam(value = "activityType", required = false) String activityType,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(parentControlService.getActivityLogs(childAccount, activityType, category, status, startDate, endDate, search, pageRequest));
    }

    @GetMapping("/activity-log/export")
    public ResponseEntity<byte[]> exportActivityLogCsv(
            @RequestParam(value = "childAccount", required = false) String childAccount) {

        Page<ParentActivityLog> logs = parentControlService.getActivityLogs(childAccount, null, null, null, null, null, null, PageRequest.of(0, 1000));

        StringBuilder csv = new StringBuilder();
        csv.append("Event ID,Date & Time,Child Account,Activity Type,Category,Amount,Description,Status,Reason,Source,Actor\n");

        for (ParentActivityLog log : logs.getContent()) {
            csv.append(log.getId()).append(",")
               .append("\"").append(log.getTimestamp()).append("\",")
               .append("\"").append(log.getChildAccount()).append("\",")
               .append("\"").append(log.getActivityType()).append("\",")
               .append("\"").append(log.getCategory() != null ? log.getCategory() : "").append("\",")
               .append(log.getAmount() != null ? log.getAmount() : "").append(",")
               .append("\"").append(log.getDescription().replace("\"", "\"\"")).append("\",")
               .append("\"").append(log.getStatus()).append("\",")
               .append("\"").append(log.getReason() != null ? log.getReason().replace("\"", "\"\"") : "").append("\",")
               .append("\"").append(log.getSource()).append("\",")
               .append("\"").append(log.getActor()).append("\"\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=parent_activity_log.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<ParentAlert>> getAlerts() {
        return ResponseEntity.ok(parentControlService.getAlerts());
    }

    @PostMapping("/alerts/{id}/read")
    public ResponseEntity<Void> markAlertRead(@PathVariable Long id) {
        parentControlService.markAlertRead(id);
        return ResponseEntity.ok().build();
    }
}
