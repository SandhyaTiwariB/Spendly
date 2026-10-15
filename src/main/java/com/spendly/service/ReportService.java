package com.spendly.service;

import com.spendly.dto.BudgetComparisonDTO;
import com.spendly.dto.CategoryBreakdownDTO;
import com.spendly.dto.FinancialReportResponse;
import com.spendly.entity.Budget;
import com.spendly.entity.Transaction;
import com.spendly.repository.BudgetRepository;
import com.spendly.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final FinancialCalculationService calculationService;

    public ReportService(TransactionRepository transactionRepository,
                         BudgetRepository budgetRepository,
                         FinancialCalculationService calculationService) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.calculationService = calculationService;
    }

    public FinancialReportResponse getReport(String period, LocalDate start, LocalDate end) {
        FinancialCalculationService.DateRange range = calculationService.resolveDateRange(period, start, end);
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(
                range.getStartDate(),
                range.getEndDate()
        );

        FinancialCalculationService.SummaryTotals totals = calculationService.calculateTotals(transactions);
        List<CategoryBreakdownDTO> breakdown = calculationService.calculateCategoryBreakdown(transactions);

        String highestCategory = !breakdown.isEmpty() ? breakdown.get(0).getCategory() : "None";
        BigDecimal highestAmount = !breakdown.isEmpty() ? breakdown.get(0).getAmount() : BigDecimal.ZERO;

        // Calculate comparison with previous period
        long days = java.time.temporal.ChronoUnit.DAYS.between(range.getStartDate(), range.getEndDate()) + 1;
        LocalDate prevStart = range.getStartDate().minusDays(days);
        LocalDate prevEnd = range.getStartDate().minusDays(1);
        List<Transaction> prevTransactions = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(prevStart, prevEnd);
        FinancialCalculationService.SummaryTotals prevTotals = calculationService.calculateTotals(prevTransactions);

        Double incomeChange = null;
        if (prevTotals.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = totals.getTotalIncome().subtract(prevTotals.getTotalIncome());
            incomeChange = diff.divide(prevTotals.getTotalIncome(), 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
        }

        Double expenseChange = null;
        if (prevTotals.getTotalExpenses().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = totals.getTotalExpenses().subtract(prevTotals.getTotalExpenses());
            expenseChange = diff.divide(prevTotals.getTotalExpenses(), 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
        }

        // Budget vs Actual for the month of range start date
        String yearMonth = range.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Budget> budgets = budgetRepository.findByYearMonth(yearMonth);
        Map<String, BigDecimal> actualMap = new HashMap<>();
        for (CategoryBreakdownDTO c : breakdown) {
            actualMap.put(c.getCategory().toLowerCase(), c.getAmount());
        }

        List<BudgetComparisonDTO> budgetComparisons = new ArrayList<>();
        for (Budget b : budgets) {
            BigDecimal spent = actualMap.getOrDefault(b.getCategory().toLowerCase(), BigDecimal.ZERO);
            BigDecimal rem = b.getMonthlyLimit().subtract(spent);
            double pct = b.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0
                    ? spent.divide(b.getMonthlyLimit(), 4, RoundingMode.HALF_UP).doubleValue() * 100.0
                    : 0.0;
            boolean isOver = spent.compareTo(b.getMonthlyLimit()) >= 0;
            budgetComparisons.add(new BudgetComparisonDTO(
                    b.getCategory(),
                    b.getMonthlyLimit(),
                    spent,
                    rem,
                    Math.round(pct * 10.0) / 10.0,
                    isOver
            ));
        }

        FinancialReportResponse report = new FinancialReportResponse();
        report.setPeriod(period != null ? period : "month");
        report.setStartDate(range.getStartDate());
        report.setEndDate(range.getEndDate());
        report.setTotalIncome(totals.getTotalIncome());
        report.setTotalExpenses(totals.getTotalExpenses());
        report.setNetCashFlow(totals.getNetCashFlow());
        report.setSavingsRate(totals.getSavingsRate());
        report.setHighestSpendingCategory(highestCategory);
        report.setHighestSpendingAmount(highestAmount);
        report.setCategoryBreakdown(breakdown);
        report.setBudgetVsActual(budgetComparisons);
        report.setPreviousPeriodIncome(prevTotals.getTotalIncome());
        report.setPreviousPeriodExpenses(prevTotals.getTotalExpenses());
        report.setIncomeChangePercentage(incomeChange);
        report.setExpenseChangePercentage(expenseChange);

        return report;
    }
}
