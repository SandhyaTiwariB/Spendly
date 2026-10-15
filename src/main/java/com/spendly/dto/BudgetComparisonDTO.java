package com.spendly.dto;

import java.math.BigDecimal;

public class BudgetComparisonDTO {
    private String category;
    private BigDecimal budgetLimit;
    private BigDecimal actualSpent;
    private BigDecimal remaining;
    private double percentageUsed;
    private boolean isOverBudget;

    public BudgetComparisonDTO() {
    }

    public BudgetComparisonDTO(String category, BigDecimal budgetLimit, BigDecimal actualSpent, BigDecimal remaining, double percentageUsed, boolean isOverBudget) {
        this.category = category;
        this.budgetLimit = budgetLimit;
        this.actualSpent = actualSpent;
        this.remaining = remaining;
        this.percentageUsed = percentageUsed;
        this.isOverBudget = isOverBudget;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public BigDecimal getActualSpent() {
        return actualSpent;
    }

    public void setActualSpent(BigDecimal actualSpent) {
        this.actualSpent = actualSpent;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public void setRemaining(BigDecimal remaining) {
        this.remaining = remaining;
    }

    public double getPercentageUsed() {
        return percentageUsed;
    }

    public void setPercentageUsed(double percentageUsed) {
        this.percentageUsed = percentageUsed;
    }

    public boolean isOverBudget() {
        return isOverBudget;
    }

    public void setOverBudget(boolean overBudget) {
        isOverBudget = overBudget;
    }
}
