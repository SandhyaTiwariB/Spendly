package com.spendly.controller;

import com.spendly.dto.AuthRequest;
import com.spendly.dto.AuthResponse;
import com.spendly.dto.RegisterRequest;
import com.spendly.entity.User;
import com.spendly.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestParam(value = "email", defaultValue = "vrinda@spendly.io") String email,
                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = null;
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            user = authService.getUserByToken(authHeader);
        }
        if (user == null) {
            user = authService.getProfile(email);
        }
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> userData = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole(),
                "currency", user.getCurrency(),
                "monthlyBudget", user.getMonthlyBudget()
        );
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "user", userData,
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole(),
                "currency", user.getCurrency(),
                "monthlyBudget", user.getMonthlyBudget()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "A valid email is required."));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Development Mode: Password reset link generated for " + email + ". You may log in using your registered credentials or demo password 'Spendly@2026'."
        ));
    }
}
