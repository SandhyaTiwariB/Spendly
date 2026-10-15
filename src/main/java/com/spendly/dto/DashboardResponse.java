package com.spendly.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DashboardResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netCashFlow;
    private double savingsRate;
    private int transactionCount;
    private String period;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<CategoryBreakdownDTO> categoryBreakdown;
    private List<TransactionResponse> recentTransactions;

    public DashboardResponse() {
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

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
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

    public List<CategoryBreakdownDTO> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CategoryBreakdownDTO> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<CategoryBreakdownDTO> getCategoryBreakdowns() {
        return categoryBreakdown;
    }

    public List<TransactionResponse> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<TransactionResponse> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }

    public BigDecimal getTotalSpent() {
        return totalExpenses != null ? totalExpenses : BigDecimal.ZERO;
    }

    public BigDecimal getBudgetLimit() {
        return budgetLimit != null ? budgetLimit : new BigDecimal("40000.00");
    }

    public void setBudgetLimit(BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public BigDecimal getRemainingBudget() {
        BigDecimal limit = getBudgetLimit();
        BigDecimal spent = getTotalSpent();
        BigDecimal rem = limit.subtract(spent);
        return rem.compareTo(BigDecimal.ZERO) >= 0 ? rem : BigDecimal.ZERO;
    }

    public BigDecimal getNetLent() {
        return netLent != null ? netLent : BigDecimal.ZERO;
    }

    public void setNetLent(BigDecimal netLent) {
        this.netLent = netLent;
    }

    public List<java.util.Map<String, Object>> getOwedToYouList() {
        return owedToYouList != null ? owedToYouList : java.util.Collections.emptyList();
    }

    public void setOwedToYouList(List<java.util.Map<String, Object>> owedToYouList) {
        this.owedToYouList = owedToYouList;
    }

    public BigDecimal getNetBorrowed() {
        return netBorrowed != null ? netBorrowed : BigDecimal.ZERO;
    }

    public void setNetBorrowed(BigDecimal netBorrowed) {
        this.netBorrowed = netBorrowed;
    }

    public List<java.util.Map<String, Object>> getYouOweList() {
        return youOweList != null ? youOweList : java.util.Collections.emptyList();
    }

    public void setYouOweList(List<java.util.Map<String, Object>> youOweList) {
        this.youOweList = youOweList;
    }

    public List<java.util.Map<String, Object>> getSpendingOverview() {
        return spendingOverview != null ? spendingOverview : java.util.Collections.emptyList();
    }

    public void setSpendingOverview(List<java.util.Map<String, Object>> spendingOverview) {
        this.spendingOverview = spendingOverview;
    }

    private BigDecimal budgetLimit = new BigDecimal("40000.00");
    private BigDecimal netLent = BigDecimal.ZERO;
    private List<java.util.Map<String, Object>> owedToYouList;
    private BigDecimal netBorrowed = BigDecimal.ZERO;
    private List<java.util.Map<String, Object>> youOweList;
    private List<java.util.Map<String, Object>> spendingOverview;
}
