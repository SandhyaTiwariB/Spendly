package com.spendly.service;

import com.spendly.dto.BudgetRequest;
import com.spendly.dto.BudgetResponse;
import com.spendly.dto.CategoryRequest;
import com.spendly.dto.CategoryResponse;
import com.spendly.dto.ParentRuleUpdateRequest;
import com.spendly.dto.TransactionRequest;
import com.spendly.dto.TransactionResponse;
import com.spendly.entity.CategoryType;
import com.spendly.entity.ParentActivityLog;
import com.spendly.entity.TransactionDirection;
import com.spendly.entity.User;
import com.spendly.repository.CategoryRepository;
import com.spendly.repository.ParentActivityLogRepository;
import com.spendly.repository.ParentRuleRepository;
import com.spendly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CustomCategoryAndBudgetIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private ParentControlService parentControlService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParentActivityLogRepository logRepository;

    @Autowired
    private ParentRuleRepository ruleRepository;

    private User parentUser;
    private User childUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        parentUser = userRepository.findByEmailIgnoreCase("vrinda@spendly.io")
                .orElseGet(() -> userRepository.save(new User("vrinda@spendly.io", "pass", "Vrinda", "PARENT", new BigDecimal("40000.00"))));

        childUser = userRepository.findByEmailIgnoreCase("aarav@spendly.io")
                .orElseGet(() -> userRepository.save(new User("aarav@spendly.io", "pass", "Aarav", "CHILD", new BigDecimal("12000.00"))));

        otherUser = userRepository.findByEmailIgnoreCase("other@spendly.io")
                .orElseGet(() -> userRepository.save(new User("other@spendly.io", "pass", "Other User", "PARENT", new BigDecimal("30000.00"))));
    }

    @Test
    @DisplayName("Test A — Category created from Expenses: appears in selector, persists across requests")
    void testA_CategoryCreatedFromExpenses() {
        String catName = "Pet Care " + System.currentTimeMillis();
        CategoryRequest req = new CategoryRequest(catName, CategoryType.EXPENSE, "#27ae60", "🐾");

        CategoryResponse created = categoryService.createCategory(req, parentUser);
        assertNotNull(created.getId());
        assertEquals(catName, created.getName());
        assertEquals("🐾", created.getIcon());
        assertEquals("#27ae60", created.getColor());
        assertTrue(created.isCustom());

        // Verify it appears in category list for parent
        List<CategoryResponse> list = categoryService.getCategories(parentUser);
        boolean found = list.stream().anyMatch(c -> c.getName().equals(catName));
        assertTrue(found, "Newly created category should appear in category selector");

        // Verify persistence directly from DB
        assertTrue(categoryRepository.existsByNameIgnoreCase(catName));
    }

    @Test
    @DisplayName("Test B — Category created from Budgets: available in Expenses and can assign budget")
    void testB_CategoryCreatedFromBudgets() {
        String catName = "Travel - Personal " + System.currentTimeMillis();
        CategoryRequest req = new CategoryRequest(catName, CategoryType.EXPENSE, "#3498db", "✈️");

        CategoryResponse created = categoryService.createCategory(req, parentUser);
        assertNotNull(created.getId());

        // Verify category is available in Expenses
        List<CategoryResponse> categories = categoryService.getCategories(parentUser);
        assertTrue(categories.stream().anyMatch(c -> c.getName().equalsIgnoreCase(catName)));

        // Create budget for this category
        String ym = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BudgetRequest budgetReq = new BudgetRequest(catName, new BigDecimal("8000.00"), ym);
        BudgetResponse budget = budgetService.saveBudget(budgetReq);

        assertNotNull(budget.getId());
        assertEquals(catName, budget.getCategory());
        assertEquals(new BigDecimal("8000.00"), budget.getMonthlyLimit());
        assertEquals(BigDecimal.ZERO.setScale(2), budget.getSpentAmount());
        assertEquals(new BigDecimal("8000.00"), budget.getRemainingAmount());
    }

    @Test
    @DisplayName("Test C — Shared calculations: transaction in custom category updates budget spending and breakdown")
    void testC_SharedCalculations() {
        String catName = "Fitness & Sports " + System.currentTimeMillis();
        categoryService.createCategory(new CategoryRequest(catName, CategoryType.EXPENSE, "#e74c3c", "🏋️"), parentUser);

        String ym = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        budgetService.saveBudget(new BudgetRequest(catName, new BigDecimal("5000.00"), ym));

        // Create an expense transaction in this custom category
        TransactionRequest tx = new TransactionRequest();
        tx.setDescription("Gym Membership Annual");
        tx.setAmount(new BigDecimal("2400.00"));
        tx.setDirection(TransactionDirection.DEBIT);
        tx.setCategory(catName);
        tx.setTransactionDate(LocalDate.now());

        TransactionResponse savedTx = transactionService.createTransaction(tx);
        assertNotNull(savedTx.getId());
        assertEquals(catName, savedTx.getCategory());

        // Verify budget spending updated
        List<BudgetResponse> budgets = budgetService.getBudgets(ym);
        BudgetResponse customBudget = budgets.stream()
                .filter(b -> b.getCategory().equalsIgnoreCase(catName))
                .findFirst()
                .orElse(null);

        assertNotNull(customBudget);
        assertEquals(new BigDecimal("2400.00"), customBudget.getSpentAmount());
        assertEquals(new BigDecimal("2600.00"), customBudget.getRemainingAmount());
        assertEquals(48.0, customBudget.getPercentageUsed());
    }

    @Test
    @DisplayName("Test D — Validation: empty name and duplicate name are rejected")
    void testD_Validation() {
        // 1. Empty name
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(new CategoryRequest("   ", CategoryType.EXPENSE, "#000", "•"), parentUser);
        });

        // 2. Duplicate default category name (case-insensitive)
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(new CategoryRequest("food & dining", CategoryType.EXPENSE, "#000", "•"), parentUser);
        });

        // 3. Duplicate custom category name (case-insensitive)
        String unique = "Custom Tag " + System.currentTimeMillis();
        categoryService.createCategory(new CategoryRequest(unique, CategoryType.EXPENSE, "#000", "•"), parentUser);

        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(new CategoryRequest(unique.toLowerCase(), CategoryType.EXPENSE, "#000", "•"), parentUser);
        });
    }

    @Test
    @DisplayName("Test E — Account isolation: private custom category does not leak to other users")
    void testE_AccountIsolation() {
        String privateCat = "Secret Fund " + System.currentTimeMillis();
        categoryService.createCategory(new CategoryRequest(privateCat, CategoryType.EXPENSE, "#000", "•"), parentUser);

        // Parent sees it
        List<CategoryResponse> parentList = categoryService.getCategories(parentUser);
        assertTrue(parentList.stream().anyMatch(c -> c.getName().equals(privateCat)));

        // Unrelated other user should NOT see it
        List<CategoryResponse> otherList = categoryService.getCategories(otherUser);
        assertFalse(otherList.stream().anyMatch(c -> c.getName().equals(privateCat)), "Private custom category should not leak to unrelated user");
    }

    @Test
    @DisplayName("Test F — Parent Control: restricted custom category transaction is rejected and logged")
    void testF_ParentControl() {
        String catName = "Video Games " + System.currentTimeMillis();
        categoryService.createCategory(new CategoryRequest(catName, CategoryType.EXPENSE, "#8e44ad", "🎮"), parentUser);

        // Child account is not allowed to create categories
        assertThrows(SecurityException.class, () -> {
            categoryService.createCategory(new CategoryRequest("Bypass " + System.currentTimeMillis(), CategoryType.EXPENSE, "#000", "•"), childUser);
        });

        // Disable category permission for child
        ParentRuleUpdateRequest updateReq = new ParentRuleUpdateRequest();
        updateReq.setEnabled(false);
        parentControlService.updateRule("Aarav's account", catName, updateReq);

        // Verify child spending is blocked
        assertFalse(parentControlService.isCategoryAllowed("Aarav's account", catName));

        // Attempting to create transaction in disabled category must be rejected
        TransactionRequest tx = new TransactionRequest();
        tx.setDescription("Steam Game Bundle");
        tx.setAmount(new BigDecimal("1299.00"));
        tx.setDirection(TransactionDirection.DEBIT);
        tx.setCategory(catName);
        tx.setTransactionDate(LocalDate.now());

        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.createTransaction(tx);
        });

        // Verify it was logged in Activity Log
        List<ParentActivityLog> logs = logRepository.findByChildAccountOrderByTimestampDesc("Aarav's account");
        boolean blockedLogged = logs.stream().anyMatch(l -> "TRANSACTION_BLOCKED".equals(l.getActivityType()) && catName.equalsIgnoreCase(l.getCategory()));
        assertTrue(blockedLogged, "Blocked transaction must be recorded in Activity Log");
    }

    @Test
    @DisplayName("Test G — Regression: existing default categories and budgets function correctly")
    void testG_Regression() {
        List<CategoryResponse> categories = categoryService.getCategories(parentUser);
        assertTrue(categories.size() >= 10);
        assertTrue(categories.stream().anyMatch(c -> c.getName().equalsIgnoreCase("Food & Dining")));
        assertTrue(categories.stream().anyMatch(c -> c.getName().equalsIgnoreCase("Shopping")));
        assertTrue(categories.stream().anyMatch(c -> c.getName().equalsIgnoreCase("Income")));

        // Can get budgets
        List<BudgetResponse> budgets = budgetService.getBudgets(null);
        assertNotNull(budgets);
    }
}
