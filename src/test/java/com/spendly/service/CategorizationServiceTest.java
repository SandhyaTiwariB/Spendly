package com.spendly.service;

import com.spendly.entity.TransactionDirection;
import com.spendly.entity.TransactionType;
import com.spendly.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class CategorizationServiceTest {

    private CategorizationService service;

    @BeforeEach
    void setUp() {
        CategoryRepository mockRepo = Mockito.mock(CategoryRepository.class);
        service = new CategorizationService(mockRepo);
    }

    @Test
    void testRuleBasedCategorization() {
        // Salary
        CategorizationService.CategoryResult salary = service.categorize("Monthly salary credit", TransactionDirection.CREDIT);
        assertEquals("Income", salary.getCategory());
        assertEquals(TransactionType.INCOME, salary.getType());
        assertFalse(salary.isTransfer());

        // Food & Dining
        CategorizationService.CategoryResult food = service.categorize("Cafe coffee and lunch", TransactionDirection.DEBIT);
        assertEquals("Food & Dining", food.getCategory());
        assertEquals(TransactionType.EXPENSE, food.getType());
        assertFalse(food.isTransfer());

        // Transport
        CategorizationService.CategoryResult taxi = service.categorize("City taxi ride", TransactionDirection.DEBIT);
        assertEquals("Transport", taxi.getCategory());
        assertEquals(TransactionType.EXPENSE, taxi.getType());

        // Shopping
        CategorizationService.CategoryResult shop = service.categorize("Clothing store Zara", TransactionDirection.DEBIT);
        assertEquals("Shopping", shop.getCategory());
        assertEquals(TransactionType.EXPENSE, shop.getType());

        // Internal Transfer
        CategorizationService.CategoryResult trf = service.categorize("Transfer to own savings", TransactionDirection.DEBIT);
        assertEquals("Transfer", trf.getCategory());
        assertEquals(TransactionType.TRANSFER, trf.getType());
        assertTrue(trf.isTransfer());
    }
}
