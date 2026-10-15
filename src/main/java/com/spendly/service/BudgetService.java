package com.spendly.service;

import com.spendly.dto.BudgetRequest;
import com.spendly.dto.BudgetResponse;
import com.spendly.entity.Budget;
import com.spendly.entity.Transaction;
import com.spendly.entity.TransactionType;
import com.spendly.exception.ResourceNotFoundException;
import com.spendly.repository.BudgetRepository;
import com.spendly.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<BudgetResponse> getBudgets(String yearMonth) {
        String targetMonth = yearMonth != null && !yearMonth.trim().isEmpty()
                ? yearMonth.trim()
                : YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<Budget> budgets = budgetRepository.findAllByOrderByYearMonthDescCategoryAsc();
        // If yearMonth filter provided, filter or return all if empty
        if (yearMonth != null && !yearMonth.trim().isEmpty()) {
            budgets = budgets.stream().filter(b -> b.getYearMonth().equals(targetMonth)).collect(Collectors.toList());
        }

        return budgets.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public BudgetResponse getBudgetById(Long id) {
        Budget b = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + id));
        return toResponse(b);
    }

    @Transactional
    public BudgetResponse saveBudget(BudgetRequest request) {
        if (request.getMonthlyLimit() == null || request.getMonthlyLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly budget limit must be greater than zero.");
        }

        String ym = request.getYearMonth() != null && !request.getYearMonth().trim().isEmpty()
                ? request.getYearMonth().trim()
                : YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        String cat = request.getCategory().trim();

        // Check if budget for same category and month exists -> update it (or create new)
        Optional<Budget> existing = budgetRepository.findByCategoryIgnoreCaseAndYearMonth(cat, ym);
        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setMonthlyLimit(request.getMonthlyLimit());
        } else {
            budget = new Budget(cat, ym, request.getMonthlyLimit());
        }

        Budget saved = budgetRepository.save(budget);
        return toResponse(saved);
    }

    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        Budget b = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + id));

        if (request.getMonthlyLimit() != null && request.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0) {
            b.setMonthlyLimit(request.getMonthlyLimit());
        }
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            b.setCategory(request.getCategory().trim());
        }
        if (request.getYearMonth() != null && !request.getYearMonth().trim().isEmpty()) {
            b.setYearMonth(request.getYearMonth().trim());
        }

        Budget updated = budgetRepository.save(b);
        return toResponse(updated);
    }

    @Transactional
    public void deleteBudget(Long id) {
        if (!budgetRepository.existsById(id)) {
            throw new ResourceNotFoundException("Budget not found with ID: " + id);
        }
        budgetRepository.deleteById(id);
    }

    public BudgetResponse toResponse(Budget b) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(b.getYearMonth());
        } catch (Exception e) {
            ym = YearMonth.now();
        }

        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(startDate, endDate);

        BigDecimal spent = BigDecimal.ZERO;
        for (Transaction tx : transactions) {
            if (!tx.isTransfer() && tx.getType() == TransactionType.EXPENSE && tx.getCategory().equalsIgnoreCase(b.getCategory())) {
                spent = spent.add(tx.getAmount());
            }
        }
        spent = spent.setScale(2, RoundingMode.HALF_UP);

        BigDecimal limit = b.getMonthlyLimit().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = limit.subtract(spent).setScale(2, RoundingMode.HALF_UP);

        double pct = limit.compareTo(BigDecimal.ZERO) > 0
                ? spent.divide(limit, 4, RoundingMode.HALF_UP).doubleValue() * 100.0
                : 0.0;

        boolean isExceeded = spent.compareTo(limit) >= 0;
        boolean isApproaching = !isExceeded && pct >= 80.0;
        String status = isExceeded ? "EXCEEDED" : isApproaching ? "WARNING" : "NORMAL";

        BudgetResponse resp = new BudgetResponse();
        resp.setId(b.getId());
        resp.setCategory(b.getCategory());
        resp.setYearMonth(b.getYearMonth());
        resp.setMonthlyLimit(limit);
        resp.setLimitAmount(limit);
        resp.setSpentAmount(spent);
        resp.setRemainingAmount(remaining);
        resp.setPercentageUsed(Math.round(pct * 10.0) / 10.0);
        resp.setApproachingLimit(isApproaching);
        resp.setExceeded(isExceeded);
        resp.setStatus(status);

        return resp;
    }
}
