package com.spendly.service;

import com.spendly.dto.CategoryBreakdownDTO;
import com.spendly.dto.DashboardResponse;
import com.spendly.dto.TransactionResponse;
import com.spendly.entity.LendingObligation;
import com.spendly.entity.ObligationType;
import com.spendly.entity.Transaction;
import com.spendly.repository.LendingObligationRepository;
import com.spendly.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final FinancialCalculationService calculationService;
    private final LendingObligationRepository lendingObligationRepository;
    private final DemoDataService demoDataService;

    public DashboardService(TransactionRepository transactionRepository,
                            FinancialCalculationService calculationService,
                            LendingObligationRepository lendingObligationRepository,
                            DemoDataService demoDataService) {
        this.transactionRepository = transactionRepository;
        this.calculationService = calculationService;
        this.lendingObligationRepository = lendingObligationRepository;
        this.demoDataService = demoDataService;
    }

    public DashboardResponse getDashboardData(String period, LocalDate start, LocalDate end) {
        // Automatically seed standard reference dataset if the database has 0 transactions
        if (transactionRepository.count() == 0) {
            demoDataService.loadStandardSampleDataset();
        }

        FinancialCalculationService.DateRange range = calculationService.resolveDateRange(period, start, end);
        
        List<Transaction> calculationSet;
        if ("all".equalsIgnoreCase(period) || "overall".equalsIgnoreCase(period) || "all-time".equalsIgnoreCase(period)) {
            calculationSet = transactionRepository.findAllByOrderByTransactionDateDescIdDesc();
        } else {
            calculationSet = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(
                    range.getStartDate(),
                    range.getEndDate()
            );
        }

        FinancialCalculationService.SummaryTotals totals = calculationService.calculateTotals(calculationSet);
        List<CategoryBreakdownDTO> breakdown = calculationService.calculateCategoryBreakdown(calculationSet);

        List<TransactionResponse> recent = calculationSet.stream()
                .limit(5)
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());

        DashboardResponse resp = new DashboardResponse();
        resp.setTotalIncome(totals.getTotalIncome());
        resp.setTotalExpenses(totals.getTotalExpenses());
        resp.setNetCashFlow(totals.getNetCashFlow());
        resp.setSavingsRate(totals.getSavingsRate());
        resp.setTransactionCount(calculationSet.size());
        resp.setPeriod(period != null ? period : "month");
        resp.setStartDate(range.getStartDate());
        resp.setEndDate(range.getEndDate());
        resp.setCategoryBreakdown(breakdown);
        resp.setRecentTransactions(recent);
        resp.setBudgetLimit(new BigDecimal("40000.00"));

        // Social Finance: Lending & Borrowing totals
        List<LendingObligation> obligations = lendingObligationRepository.findAll();
        BigDecimal netLent = BigDecimal.ZERO;
        BigDecimal netBorrowed = BigDecimal.ZERO;
        List<Map<String, Object>> owedToYouList = new ArrayList<>();
        List<Map<String, Object>> youOweList = new ArrayList<>();

        for (LendingObligation ob : obligations) {
            if (ob.getType() == ObligationType.LENT) {
                BigDecimal bal = ob.getBalance() != null ? ob.getBalance() : ob.getAmount();
                netLent = netLent.add(bal);
                Map<String, Object> map = new HashMap<>();
                map.put("name", ob.getPersonName());
                map.put("personName", ob.getPersonName());
                map.put("amount", bal);
                owedToYouList.add(map);
            } else if (ob.getType() == ObligationType.BORROWED) {
                BigDecimal bal = ob.getBalance() != null ? ob.getBalance() : ob.getAmount();
                netBorrowed = netBorrowed.add(bal);
                Map<String, Object> map = new HashMap<>();
                map.put("name", ob.getPersonName());
                map.put("personName", ob.getPersonName());
                map.put("amount", bal);
                youOweList.add(map);
            }
        }
        resp.setNetLent(netLent);
        resp.setOwedToYouList(owedToYouList);
        resp.setNetBorrowed(netBorrowed);
        resp.setYouOweList(youOweList);

        // Spending overview chart bars (7 milestone days matching UI)
        List<Map<String, Object>> overview = List.of(
                Map.of("label", "1", "amount", 1250, "height", 45, "current", false),
                Map.of("label", "5", "amount", 2850, "height", 65, "current", false),
                Map.of("label", "10", "amount", 1820, "height", 40, "current", false),
                Map.of("label", "15", "amount", 3450, "height", 80, "current", false),
                Map.of("label", "20", "amount", 4120, "height", 90, "current", false),
                Map.of("label", "24", "amount", 2150, "height", 60, "current", true),
                Map.of("label", "30", "amount", 980, "height", 25, "current", false)
        );
        resp.setSpendingOverview(overview);

        return resp;
    }
}

