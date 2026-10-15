package com.spendly.dto;

import java.time.LocalDate;
import java.util.List;

public class DemoDataGenerateRequest {
    private String account = "Parent"; // "Parent" (Vrinda) or "Child" (Aarav)
    private int count = 10;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> categories;
    private String transactionType = "BOTH"; // "EXPENSE", "INCOME", or "BOTH"
    private boolean includeTransfers = true;
    private boolean includeDuplicates = false;
    private boolean includeAttemptedRestricted = true;
    private boolean includeBudgetOverruns = true;
    private boolean includeSplitBillsAndLoans = true;

    public DemoDataGenerateRequest() {
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
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

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public boolean isIncludeTransfers() {
        return includeTransfers;
    }

    public void setIncludeTransfers(boolean includeTransfers) {
        this.includeTransfers = includeTransfers;
    }

    public boolean isIncludeDuplicates() {
        return includeDuplicates;
    }

    public void setIncludeDuplicates(boolean includeDuplicates) {
        this.includeDuplicates = includeDuplicates;
    }

    public boolean isIncludeAttemptedRestricted() {
        return includeAttemptedRestricted;
    }

    public void setIncludeAttemptedRestricted(boolean includeAttemptedRestricted) {
        this.includeAttemptedRestricted = includeAttemptedRestricted;
    }

    public boolean isIncludeBudgetOverruns() {
        return includeBudgetOverruns;
    }

    public void setIncludeBudgetOverruns(boolean includeBudgetOverruns) {
        this.includeBudgetOverruns = includeBudgetOverruns;
    }

    public boolean isIncludeSplitBillsAndLoans() {
        return includeSplitBillsAndLoans;
    }

    public void setIncludeSplitBillsAndLoans(boolean includeSplitBillsAndLoans) {
        this.includeSplitBillsAndLoans = includeSplitBillsAndLoans;
    }
}
