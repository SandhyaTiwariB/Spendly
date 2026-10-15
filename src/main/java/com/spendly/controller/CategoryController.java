package com.spendly.controller;

import com.spendly.dto.CategoryRequest;
import com.spendly.dto.CategoryResponse;
import com.spendly.entity.User;
import com.spendly.service.AuthService;
import com.spendly.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthService authService;

    public CategoryController(CategoryService categoryService, AuthService authService) {
        this.categoryService = categoryService;
        this.authService = authService;
    }

    private User resolveUser(String authHeader, String email) {
        User user = null;
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            user = authService.getUserByToken(authHeader);
        }
        if (user == null && email != null && !email.trim().isEmpty()) {
            user = authService.getProfile(email.trim());
        }
        if (user == null) {
            user = authService.getProfile("vrinda@spendly.io");
        }
        return user;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "email", required = false) String email) {
        User user = resolveUser(authHeader, email);
        return ResponseEntity.ok(categoryService.getCategories(user));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "email", required = false) String email) {
        User user = resolveUser(authHeader, email);
        CategoryResponse created = categoryService.createCategory(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "email", required = false) String email) {
        User user = resolveUser(authHeader, email);
        categoryService.deleteCategory(id, user);
        return ResponseEntity.noContent().build();
    }
}
