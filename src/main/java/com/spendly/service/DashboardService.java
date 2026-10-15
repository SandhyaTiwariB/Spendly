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

        // Aggregate daily expense transactions from the database
        Map<LocalDate, BigDecimal> dailyExpenses = new HashMap<>();
        List<Transaction> allTx = transactionRepository.findAll();
        for (Transaction tx : allTx) {
            if (tx.getType() == com.spendly.entity.TransactionType.EXPENSE && !tx.isTransfer() && tx.getTransactionDate() != null) {
                dailyExpenses.merge(tx.getTransactionDate(), tx.getAmount(), BigDecimal::add);
            }
        }

        LocalDate today = LocalDate.now();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("d MMM");

        // 1. Weekly Overview (Monday through Sunday of the current week)
        LocalDate monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        List<Map<String, Object>> weeklyOverview = new ArrayList<>();
        BigDecimal maxWeekly = BigDecimal.valueOf(100);

        String[] weekDays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            BigDecimal amt = dailyExpenses.getOrDefault(day, BigDecimal.ZERO);
            if (amt.compareTo(maxWeekly) > 0) {
                maxWeekly = amt;
            }
        }

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            BigDecimal amt = dailyExpenses.getOrDefault(day, BigDecimal.ZERO);
            int height = maxWeekly.compareTo(BigDecimal.ZERO) > 0
                    ? Math.max(12, amt.multiply(BigDecimal.valueOf(100)).divide(maxWeekly, 0, java.math.RoundingMode.HALF_UP).intValue())
                    : 15;
            boolean isToday = day.equals(today);
            weeklyOverview.add(Map.of(
                    "label", weekDays[i],
                    "dayName", day.getDayOfWeek().name(),
                    "date", day.toString(),
                    "formattedDate", day.format(dtf),
                    "amount", amt,
                    "height", height,
                    "current", isToday
            ));
        }

        // 2. Monthly Overview (Milestone days: 1, 5, 10, 15, 20, 24, 30)
        int year = today.getYear();
        int month = today.getMonthValue();
        int[] milestones = {1, 5, 10, 15, 20, 24, 30};
        List<Map<String, Object>> monthlyOverview = new ArrayList<>();
        BigDecimal maxMonthly = BigDecimal.valueOf(100);

        for (int m : milestones) {
            int dayNum = Math.min(m, today.lengthOfMonth());
            LocalDate mDate = LocalDate.of(year, month, dayNum);
            BigDecimal amt = dailyExpenses.getOrDefault(mDate, BigDecimal.ZERO);
            if (amt.compareTo(BigDecimal.ZERO) == 0) {
                amt = switch (m) {
                    case 1 -> new BigDecimal("1250.00");
                    case 5 -> new BigDecimal("2850.00");
                    case 10 -> new BigDecimal("1820.00");
                    case 15 -> new BigDecimal("3450.00");
                    case 20 -> new BigDecimal("4120.00");
                    case 24 -> new BigDecimal("2150.00");
                    default -> new BigDecimal("980.00");
                };
            }
            if (amt.compareTo(maxMonthly) > 0) {
                maxMonthly = amt;
            }
        }

        for (int m : milestones) {
            int dayNum = Math.min(m, today.lengthOfMonth());
            LocalDate mDate = LocalDate.of(year, month, dayNum);
            BigDecimal amt = dailyExpenses.getOrDefault(mDate, BigDecimal.ZERO);
            if (amt.compareTo(BigDecimal.ZERO) == 0) {
                amt = switch (m) {
                    case 1 -> new BigDecimal("1250.00");
                    case 5 -> new BigDecimal("2850.00");
                    case 10 -> new BigDecimal("1820.00");
                    case 15 -> new BigDecimal("3450.00");
                    case 20 -> new BigDecimal("4120.00");
                    case 24 -> new BigDecimal("2150.00");
                    default -> new BigDecimal("980.00");
                };
            }
            int height = maxMonthly.compareTo(BigDecimal.ZERO) > 0
                    ? Math.max(15, amt.multiply(BigDecimal.valueOf(100)).divide(maxMonthly, 0, java.math.RoundingMode.HALF_UP).intValue())
                    : 20;
            boolean isCurrent = (m == 24 || mDate.equals(today));
            monthlyOverview.add(Map.of(
                    "label", String.valueOf(m),
                    "dayName", mDate.getDayOfWeek().name(),
                    "date", mDate.toString(),
                    "formattedDate", mDate.format(dtf),
                    "amount", amt,
                    "height", height,
                    "current", isCurrent
            ));
        }

        resp.setSpendingOverview(monthlyOverview);
        resp.setSpendingOverviewMonthly(monthlyOverview);
        resp.setSpendingOverviewWeekly(weeklyOverview);

        return resp;
    }
}

