package com.spendly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendly.dto.BudgetRequest;
import com.spendly.dto.TransactionPatchRequest;
import com.spendly.dto.TransactionRequest;
import com.spendly.entity.TransactionDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpendlyApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").exists());
    }

    @Test
    void testCategoriesEndpoint() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[?(@.name == 'Food & Dining')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Income')]").exists());
    }

    @Test
    void testEndToEndStatementImportAndRecalculation() throws Exception {
        String csvContent = "Date,Narration,Withdrawal,Deposit,Balance\n" +
                "02/10/2026,Monthly salary,0.00,40000.00,40000.00\n" +
                "03/10/2026,Cafe coffee and lunch,420.00,0.00,39580.00\n" +
                "04/10/2026,City taxi ride,280.00,0.00,39300.00\n" +
                "05/10/2026,Clothing store,1850.00,0.00,37450.00\n" +
                "06/10/2026,Transfer to own savings,2000.00,0.00,35450.00\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "integration_test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // 1. Import Statement
        mockMvc.perform(multipart("/api/statements/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedCount").value(5))
                .andExpect(jsonPath("$.duplicateCount").value(0));

        // 2. Verify Transactions List
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))));

        // 3. Verify Dashboard Totals: Income = 40,000, Expenses = 2,550, Net = 37,450
        mockMvc.perform(get("/api/dashboard?period=month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(40000.0))
                .andExpect(jsonPath("$.totalExpenses").value(2550.0))
                .andExpect(jsonPath("$.netCashFlow").value(37450.0));

        // 4. Verify Reports
        mockMvc.perform(get("/api/reports?period=month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(40000.0))
                .andExpect(jsonPath("$.totalExpenses").value(2550.0))
                .andExpect(jsonPath("$.highestSpendingCategory").value("Shopping"));

        // 5. Create Budget
        BudgetRequest budgetRequest = new BudgetRequest();
        budgetRequest.setCategory("Food & Dining");
        budgetRequest.setMonthlyLimit(new BigDecimal("1000.00"));
        budgetRequest.setYearMonth("2026-10");

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monthlyLimit").value(1000.0))
                .andExpect(jsonPath("$.spentAmount").value(420.0))
                .andExpect(jsonPath("$.remainingAmount").value(580.0))
                .andExpect(jsonPath("$.percentageUsed").value(42.0));
    }
}
