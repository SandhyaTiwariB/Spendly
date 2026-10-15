package com.spendly.service;

import com.spendly.dto.CategoryBreakdownDTO;
import com.spendly.entity.Transaction;
import com.spendly.entity.TransactionDirection;
import com.spendly.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinancialCalculationServiceTest {

    private final FinancialCalculationService service = new FinancialCalculationService();

    @Test
    void testFinancialCalculationsWithInternalTransferExclusion() {
        List<Transaction> list = new ArrayList<>();

        // Salary credit: ₹40,000
        Transaction salary = new Transaction();
        salary.setTransactionDate(LocalDate.of(2026, 10, 2));
        salary.setDescription("Monthly salary");
        salary.setAmount(new BigDecimal("40000.00"));
        salary.setDirection(TransactionDirection.CREDIT);
        salary.setType(TransactionType.INCOME);
        salary.setCategory("Income");
        salary.setTransfer(false);
        list.add(salary);

        // Food expense: ₹420
        Transaction food = new Transaction();
        food.setTransactionDate(LocalDate.of(2026, 10, 3));
        food.setDescription("Cafe coffee and lunch");
        food.setAmount(new BigDecimal("420.00"));
        food.setDirection(TransactionDirection.DEBIT);
        food.setType(TransactionType.EXPENSE);
        food.setCategory("Food & Dining");
        food.setTransfer(false);
        list.add(food);

        // Taxi expense: ₹280
        Transaction taxi = new Transaction();
        taxi.setTransactionDate(LocalDate.of(2026, 10, 4));
        taxi.setDescription("City taxi");
        taxi.setAmount(new BigDecimal("280.00"));
        taxi.setDirection(TransactionDirection.DEBIT);
        taxi.setType(TransactionType.EXPENSE);
        taxi.setCategory("Transport");
        taxi.setTransfer(false);
        list.add(taxi);

        // Shopping expense: ₹1,850
        Transaction shopping = new Transaction();
        shopping.setTransactionDate(LocalDate.of(2026, 10, 5));
        shopping.setDescription("Clothing store");
        shopping.setAmount(new BigDecimal("1850.00"));
        shopping.setDirection(TransactionDirection.DEBIT);
        shopping.setType(TransactionType.EXPENSE);
        shopping.setCategory("Shopping");
        shopping.setTransfer(false);
        list.add(shopping);

        // Internal account transfer: ₹2,000
        Transaction transfer = new Transaction();
        transfer.setTransactionDate(LocalDate.of(2026, 10, 6));
        transfer.setDescription("Transfer to own savings");
        transfer.setAmount(new BigDecimal("2000.00"));
        transfer.setDirection(TransactionDirection.DEBIT);
        transfer.setType(TransactionType.TRANSFER);
        transfer.setCategory("Transfer");
        transfer.setTransfer(true);
        list.add(transfer);

        FinancialCalculationService.SummaryTotals totals = service.calculateTotals(list);

        // Expected:
        // Total income: 40,000
        // Total expenses: 420 + 280 + 1850 = 2,550
        // Net cash flow: 40,000 - 2,550 = 37,450
        // Transfers excluded from income & expenses
        assertEquals(new BigDecimal("40000.00"), totals.getTotalIncome());
        assertEquals(new BigDecimal("2550.00"), totals.getTotalExpenses());
        assertEquals(new BigDecimal("37450.00"), totals.getNetCashFlow());
        assertEquals(new BigDecimal("2000.00"), totals.getTransferTotal());

        // Category breakdown should contain Food & Dining, Shopping, Transport, but NOT Transfer
        List<CategoryBreakdownDTO> breakdown = service.calculateCategoryBreakdown(list);
        assertEquals(3, breakdown.size());
        assertTrue(breakdown.stream().noneMatch(c -> c.getCategory().equalsIgnoreCase("Transfer")));
        assertEquals("Shopping", breakdown.get(0).getCategory());
        assertEquals(new BigDecimal("1850.00"), breakdown.get(0).getAmount());
    }
}
