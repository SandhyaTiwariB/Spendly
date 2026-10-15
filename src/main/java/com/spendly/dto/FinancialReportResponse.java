package com.spendly.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FinancialReportResponse {
    private String period;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netCashFlow;
    private double savingsRate;
    private String highestSpendingCategory;
    private BigDecimal highestSpendingAmount;
    private List<CategoryBreakdownDTO> categoryBreakdown;
    private List<BudgetComparisonDTO> budgetVsActual;
    private BigDecimal previousPeriodIncome;
    private BigDecimal previousPeriodExpenses;
    private Double incomeChangePercentage;
    private Double expenseChangePercentage;

    public FinancialReportResponse() {
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getNetCashFlow() {
        return netCashFlow;
    }

    public void setNetCashFlow(BigDecimal netCashFlow) {
        this.netCashFlow = netCashFlow;
    }

    public double getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(double savingsRate) {
        this.savingsRate = savingsRate;
    }

    public String getHighestSpendingCategory() {
        return highestSpendingCategory;
    }

    public void setHighestSpendingCategory(String highestSpendingCategory) {
        this.highestSpendingCategory = highestSpendingCategory;
    }

    public BigDecimal getHighestSpendingAmount() {
        return highestSpendingAmount;
    }

    public void setHighestSpendingAmount(BigDecimal highestSpendingAmount) {
        this.highestSpendingAmount = highestSpendingAmount;
    }

    public List<CategoryBreakdownDTO> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CategoryBreakdownDTO> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<BudgetComparisonDTO> getBudgetVsActual() {
        return budgetVsActual;
    }

    public void setBudgetVsActual(List<BudgetComparisonDTO> budgetVsActual) {
        this.budgetVsActual = budgetVsActual;
    }

    public BigDecimal getPreviousPeriodIncome() {
        return previousPeriodIncome;
    }

    public void setPreviousPeriodIncome(BigDecimal previousPeriodIncome) {
        this.previousPeriodIncome = previousPeriodIncome;
    }

    public BigDecimal getPreviousPeriodExpenses() {
        return previousPeriodExpenses;
    }

    public void setPreviousPeriodExpenses(BigDecimal previousPeriodExpenses) {
        this.previousPeriodExpenses = previousPeriodExpenses;
    }

    public Double getIncomeChangePercentage() {
        return incomeChangePercentage;
    }

    public void setIncomeChangePercentage(Double incomeChangePercentage) {
        this.incomeChangePercentage = incomeChangePercentage;
    }

    public Double getExpenseChangePercentage() {
        return expenseChangePercentage;
    }

    public void setExpenseChangePercentage(Double expenseChangePercentage) {
        this.expenseChangePercentage = expenseChangePercentage;
    }
}
