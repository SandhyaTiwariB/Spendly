package com.spendly.service;

import com.spendly.dto.CategoryRequest;
import com.spendly.dto.CategoryResponse;
import com.spendly.entity.Category;
import com.spendly.entity.CategoryType;
import com.spendly.entity.User;
import com.spendly.exception.ResourceNotFoundException;
import com.spendly.repository.BudgetRepository;
import com.spendly.repository.CategoryRepository;
import com.spendly.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final ParentControlService parentControlService;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository,
                           BudgetRepository budgetRepository,
                           ParentControlService parentControlService) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.parentControlService = parentControlService;
    }

    public List<CategoryResponse> getCategories(User user) {
        Long userId = user != null ? user.getId() : null;
        List<Category> list = categoryRepository.findVisibleCategories(userId);

        return list.stream().map(c -> {
            boolean inUse = isCategoryInUse(c.getName());
            boolean canDelete = c.isCustom() && (userId == null || (c.getUserId() != null && c.getUserId().equals(userId))) && !inUse;
            return CategoryResponse.fromEntity(c, canDelete);
        }).collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, User user) {
        if (user != null && "CHILD".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Child accounts are not permitted to create custom categories. Please request a parent to configure new categories.");
        }

        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }

        String trimmedName = request.getName().trim();
        if (trimmedName.length() > 50) {
            throw new IllegalArgumentException("Category name cannot exceed 50 characters.");
        }

        Long userId = user != null ? user.getId() : null;
        if (categoryRepository.existsByNameIgnoreCaseForUser(trimmedName, userId)) {
            throw new IllegalArgumentException("A category named '" + trimmedName + "' already exists.");
        }

        CategoryType type = request.getType() != null ? request.getType() : CategoryType.EXPENSE;
        String color = (request.getColor() != null && !request.getColor().trim().isEmpty())
                ? request.getColor().trim()
                : "#16865c";
        String icon = (request.getIcon() != null && !request.getIcon().trim().isEmpty())
                ? request.getIcon().trim()
                : "🏷️";

        Category category = new Category(trimmedName, type, color, icon, userId, true);
        Category saved = categoryRepository.save(category);

        // Synchronize with Parent Control
        try {
            parentControlService.registerNewCategoryRule(trimmedName);
        } catch (Exception ignored) {
        }

        return CategoryResponse.fromEntity(saved, true);
    }

    @Transactional
    public void deleteCategory(Long id, User user) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (!category.isCustom() || category.getUserId() == null) {
            throw new IllegalArgumentException("Default system categories cannot be deleted.");
        }

        if (user != null && !category.getUserId().equals(user.getId())) {
            throw new SecurityException("You do not have permission to delete this category.");
        }

        if (isCategoryInUse(category.getName())) {
            throw new IllegalStateException("Cannot delete category '" + category.getName() + "' because it is currently used by existing transactions or budgets. Reassign or delete those first.");
        }

        categoryRepository.delete(category);

        // Remove from Parent Control rules if present
        try {
            parentControlService.removeCategoryRule(category.getName());
        } catch (Exception ignored) {
        }
    }

    public boolean isCategoryInUse(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) return false;
        String name = categoryName.trim();
        return transactionRepository.existsByCategoryIgnoreCase(name) ||
               budgetRepository.existsByCategoryIgnoreCase(name);
    }
}
