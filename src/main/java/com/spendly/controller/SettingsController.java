package com.spendly.controller;

import com.spendly.dto.UserSettingsRequest;
import com.spendly.entity.User;
import com.spendly.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final AuthService authService;

    public SettingsController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings(
            @RequestParam(value = "email", defaultValue = "vrinda@spendly.io") String email) {
        User user = authService.getProfile(email);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("fullName", user.getFullName());
        map.put("email", user.getEmail());
        map.put("currency", user.getCurrency() != null ? user.getCurrency() : "INR — ₹");
        map.put("monthlyBudget", user.getMonthlyBudget());
        map.put("role", user.getRole());
        return ResponseEntity.ok(map);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(
            @RequestParam(value = "email", defaultValue = "vrinda@spendly.io") String email,
            @RequestBody UserSettingsRequest request) {

        User updated = authService.updateSettings(
                email,
                request.getFullName(),
                request.getCurrency(),
                request.getMonthlyBudget(),
                request.getNewPassword()
        );

        if (updated == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", "Settings updated successfully");
        map.put("fullName", updated.getFullName());
        map.put("email", updated.getEmail());
        map.put("currency", updated.getCurrency());
        map.put("monthlyBudget", updated.getMonthlyBudget());
        return ResponseEntity.ok(map);
    }
}
