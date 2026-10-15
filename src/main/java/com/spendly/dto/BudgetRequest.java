package com.spendly.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BudgetRequest {

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Budget limit amount is required")
    @DecimalMin(value = "0.01", message = "Budget limit must be greater than zero")
    private BigDecimal monthlyLimit;

    // Optional field for frontend alias
    private BigDecimal limitAmount;

    private String yearMonth;

    public BudgetRequest() {
    }

    public BudgetRequest(String category, BigDecimal monthlyLimit, String yearMonth) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.limitAmount = monthlyLimit;
        this.yearMonth = yearMonth;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryName() {
        return category;
    }

    public void setCategoryName(String categoryName) {
        this.category = categoryName;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit != null ? monthlyLimit : limitAmount;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount != null ? limitAmount : monthlyLimit;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }
}
