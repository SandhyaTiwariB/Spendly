package com.spendly.service;

import com.spendly.dto.BudgetRequest;
import com.spendly.dto.BudgetResponse;
import com.spendly.entity.Transaction;
import com.spendly.entity.TransactionDirection;
import com.spendly.entity.TransactionType;
import com.spendly.repository.BudgetRepository;
import com.spendly.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BudgetServiceTest {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Test
    void testBudgetCalculationsAndThresholdAlerts() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Setup an expense of 450 in Food & Dining
        Transaction tx = new Transaction();
        tx.setTransactionDate(LocalDate.now());
        tx.setDescription("Restaurant dinner");
        tx.setAmount(new BigDecimal("450.00"));
        tx.setDirection(TransactionDirection.DEBIT);
        tx.setType(TransactionType.EXPENSE);
        tx.setCategory("Food & Dining");
        tx.setTransfer(false);
        transactionRepository.save(tx);

        // Budget of 500 (spending is 450 / 500 = 90% -> WARNING threshold)
        BudgetRequest req = new BudgetRequest();
        req.setCategory("Food & Dining");
        req.setMonthlyLimit(new BigDecimal("500.00"));
        req.setYearMonth(currentMonth);

        BudgetResponse resp = budgetService.saveBudget(req);
        assertEquals(new BigDecimal("500.00"), resp.getMonthlyLimit());
        assertEquals(new BigDecimal("450.00"), resp.getSpentAmount());
        assertEquals(new BigDecimal("50.00"), resp.getRemainingAmount());
        assertEquals(90.0, resp.getPercentageUsed());
        assertTrue(resp.isApproachingLimit());
        assertFalse(resp.isExceeded());
        assertEquals("WARNING", resp.getStatus());

        // Now add another 100 expense (total 550 > 500 -> EXCEEDED threshold)
        Transaction tx2 = new Transaction();
        tx2.setTransactionDate(LocalDate.now());
        tx2.setDescription("Coffee snack");
        tx2.setAmount(new BigDecimal("100.00"));
        tx2.setDirection(TransactionDirection.DEBIT);
        tx2.setType(TransactionType.EXPENSE);
        tx2.setCategory("Food & Dining");
        tx2.setTransfer(false);
        transactionRepository.save(tx2);

        BudgetResponse updated = budgetService.getBudgetById(resp.getId());
        assertEquals(new BigDecimal("550.00"), updated.getSpentAmount());
        assertEquals(new BigDecimal("-50.00"), updated.getRemainingAmount());
        assertTrue(updated.isExceeded());
        assertEquals("EXCEEDED", updated.getStatus());
    }

    @Test
    void testNegativeOrZeroBudgetRejected() {
        BudgetRequest badReq = new BudgetRequest();
        badReq.setCategory("Shopping");
        badReq.setMonthlyLimit(new BigDecimal("0.00"));

        assertThrows(IllegalArgumentException.class, () -> budgetService.saveBudget(badReq));
    }
}
