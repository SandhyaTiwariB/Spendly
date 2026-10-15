package com.spendly.service;

import com.spendly.dto.CategoryBreakdownDTO;
import com.spendly.entity.Transaction;
import com.spendly.entity.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialCalculationService {

    public static class SummaryTotals {
        private final BigDecimal totalIncome;
        private final BigDecimal totalExpenses;
        private final BigDecimal netCashFlow;
        private final double savingsRate;
        private final BigDecimal transferTotal;
        private final int count;

        public SummaryTotals(BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal netCashFlow, double savingsRate, BigDecimal transferTotal, int count) {
            this.totalIncome = totalIncome;
            this.totalExpenses = totalExpenses;
            this.netCashFlow = netCashFlow;
            this.savingsRate = savingsRate;
            this.transferTotal = transferTotal;
            this.count = count;
        }

        public BigDecimal getTotalIncome() { return totalIncome; }
        public BigDecimal getTotalExpenses() { return totalExpenses; }
        public BigDecimal getNetCashFlow() { return netCashFlow; }
        public double getSavingsRate() { return savingsRate; }
        public BigDecimal getTransferTotal() { return transferTotal; }
        public int getCount() { return count; }
    }

    public static class DateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        public DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
    }

    public DateRange resolveDateRange(String period, LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return new DateRange(start, end);
        }

        LocalDate now = LocalDate.now();
        if ("all".equalsIgnoreCase(period) || "overall".equalsIgnoreCase(period) || "all-time".equalsIgnoreCase(period)) {
            return new DateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2099, 12, 31));
        } else if ("week".equalsIgnoreCase(period)) {
            LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            return new DateRange(monday, sunday);
        } else if ("year".equalsIgnoreCase(period)) {
            LocalDate jan1 = now.with(TemporalAdjusters.firstDayOfYear());
            LocalDate dec31 = now.with(TemporalAdjusters.lastDayOfYear());
            return new DateRange(jan1, dec31);
        } else {
            // Default: month
            LocalDate firstDay = now.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate lastDay = now.with(TemporalAdjusters.lastDayOfMonth());
            return new DateRange(firstDay, lastDay);
        }
    }

    public SummaryTotals calculateTotals(List<Transaction> transactions) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal transfers = BigDecimal.ZERO;
        int eligibleCount = 0;

        for (Transaction tx : transactions) {
            if (tx.isTransfer() || tx.getType() == TransactionType.TRANSFER) {
                transfers = transfers.add(tx.getAmount());
                continue;
            }

            if (tx.getType() == TransactionType.INCOME) {
                income = income.add(tx.getAmount());
                eligibleCount++;
            } else if (tx.getType() == TransactionType.EXPENSE) {
                expenses = expenses.add(tx.getAmount());
                eligibleCount++;
            }
        }

        income = income.setScale(2, RoundingMode.HALF_UP);
        expenses = expenses.setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = income.subtract(expenses).setScale(2, RoundingMode.HALF_UP);

        double savingsRate = 0.0;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = net.divide(income, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            if (savingsRate < 0) savingsRate = 0.0;
        }

        return new SummaryTotals(income, expenses, net, Math.round(savingsRate * 10.0) / 10.0, transfers, eligibleCount);
    }

    public List<CategoryBreakdownDTO> calculateCategoryBreakdown(List<Transaction> transactions) {
        Map<String, BigDecimal> categorySums = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            if (tx.isTransfer() || tx.getType() != TransactionType.EXPENSE) {
                continue;
            }
            String cat = tx.getCategory() != null ? tx.getCategory() : "Other";
            categorySums.put(cat, categorySums.getOrDefault(cat, BigDecimal.ZERO).add(tx.getAmount()));
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);
            totalExpense = totalExpense.add(tx.getAmount());
        }

        final BigDecimal finalTotal = totalExpense;
        return categorySums.entrySet().stream()
                .map(entry -> {
                    BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
                    double pct = finalTotal.compareTo(BigDecimal.ZERO) > 0
                            ? amount.divide(finalTotal, 4, RoundingMode.HALF_UP).doubleValue() * 100.0
                            : 0.0;
                    return new CategoryBreakdownDTO(
                            entry.getKey(),
                            amount,
                            Math.round(pct * 10.0) / 10.0,
                            categoryCounts.getOrDefault(entry.getKey(), 0)
                    );
                })
                .sorted(Comparator.comparing(CategoryBreakdownDTO::getAmount).reversed())
                .collect(Collectors.toList());
    }
}
