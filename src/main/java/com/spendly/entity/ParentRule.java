package com.spendly.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parent_rules")
public class ParentRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String childAccount = "Aarav's account";

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(precision = 15, scale = 2)
    private BigDecimal categoryLimit;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public ParentRule() {}

    public ParentRule(String childAccount, String category, boolean enabled, BigDecimal categoryLimit) {
        this.childAccount = childAccount;
        this.category = category;
        this.enabled = enabled;
        this.categoryLimit = categoryLimit;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChildAccount() {
        return childAccount;
    }

    public void setChildAccount(String childAccount) {
        this.childAccount = childAccount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getCategoryLimit() {
        return categoryLimit;
    }

    public void setCategoryLimit(BigDecimal categoryLimit) {
        this.categoryLimit = categoryLimit;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
