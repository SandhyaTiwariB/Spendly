package com.spendly.dto;

import java.math.BigDecimal;

public class CategoryBreakdownDTO {
    private String category;
    private BigDecimal amount;
    private double percentage;
    private int count;

    public CategoryBreakdownDTO() {
    }

    public CategoryBreakdownDTO(String category, BigDecimal amount, double percentage, int count) {
        this.category = category;
        this.amount = amount;
        this.percentage = percentage;
        this.count = count;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getName() {
        return category;
    }

    public BigDecimal getSpent() {
        return amount;
    }

    public BigDecimal getLimit() {
        if (category == null) return new BigDecimal("5000.00");
        switch (category.toLowerCase()) {
            case "food & dining": return new BigDecimal("8000.00");
            case "shopping": return new BigDecimal("10000.00");
            case "travel & transport": return new BigDecimal("4000.00");
            case "bills & utilities": return new BigDecimal("5000.00");
            case "entertainment": return new BigDecimal("4000.00");
            case "groceries": return new BigDecimal("6000.00");
            default: return new BigDecimal("5000.00");
        }
    }
}
