package com.spendly.service;

import com.spendly.dto.DemoDataGenerateRequest;
import com.spendly.dto.DemoDataResponse;
import com.spendly.entity.*;
import com.spendly.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

@Service
public class DemoDataService {

    private final TransactionRepository transactionRepository;
    private final SplitBillRepository splitBillRepository;
    private final LendingObligationRepository lendingObligationRepository;
    private final BudgetRepository budgetRepository;
    private final ParentControlService parentControlService;

    private static final List<String> DEMO_FOOD = List.of(
            "Swiggy Gourmet Feast", "Zomato Biryani Special", "Blue Tokai Coffee Roasters",
            "Starbucks Mocha Frappuccino", "Chaayos Chai & Bun Maska", "Haldiram's Executive Thali"
    );

    private static final List<String> DEMO_SHOPPING = List.of(
            "Zara Autumn Blazer", "Myntra Festive Collection", "Amazon India Electronics",
            "Uniqlo Oxford Cotton Shirt", "H&M Slim Fit Denim", "Nykaa Cosmetics & Grooming"
    );

    private static final List<String> DEMO_TRAVEL = List.of(
            "Uber Premier City Commute", "Ola Auto Ride", "Delhi Metro SmartCard Recharge",
            "Fastag Electronic Toll Payment", "IndiGo Air Travel Tickets"
    );

    private static final List<String> DEMO_BILLS = List.of(
            "Tata Power Electricity Utility", "Airtel Xstream Fiber Broadband", "Adani Gas Pipeline Bill",
            "Jio True 5G Quarterly Recharge"
    );

    private static final List<String> DEMO_ENTERTAINMENT = List.of(
            "BookMyShow IMAX Movie Tickets", "Netflix Premium 4K UHD Plan", "Spotify Individual Annual Plan",
            "PVR Gold Class Cinemas"
    );

    private static final List<String> DEMO_GAMING = List.of(
            "Steam Games Winter Battle Pass", "Google Play Store Bundle", "PlayStation Network DLC",
            "Apple App Store In-App Token"
    );

    private static final List<String> DEMO_GROCERIES = List.of(
            "Blinkit 10-Min Groceries", "Zepto Fresh Dairy & Essentials", "Nature's Basket Organic Fruits",
            "BigBasket Monthly Ration"
    );

    private static final List<String> DEMO_INCOME = List.of(
            "Monthly Corporate Salary Credit", "Freelance Software Consulting", "Quarterly Equity Mutual Fund Dividend",
            "Performance Milestone Incentive"
    );

    private static final List<String> DEMO_TRANSFERS = List.of(
            "Transfer to Emergency Savings Pot", "Self-Account Auto Sweep Transfer", "Transfer to Liquid Mutual Fund"
    );

    public DemoDataService(TransactionRepository transactionRepository,
                           SplitBillRepository splitBillRepository,
                           LendingObligationRepository lendingObligationRepository,
                           BudgetRepository budgetRepository,
                           ParentControlService parentControlService) {
        this.transactionRepository = transactionRepository;
        this.splitBillRepository = splitBillRepository;
        this.lendingObligationRepository = lendingObligationRepository;
        this.budgetRepository = budgetRepository;
        this.parentControlService = parentControlService;
    }

    @Transactional
    public DemoDataResponse generateDemoData(DemoDataGenerateRequest req) {
        Random random = new Random(42L + System.currentTimeMillis());
        String account = req.getAccount() != null && req.getAccount().equalsIgnoreCase("Child") ? "aarav@spendly.io" : "vrinda@spendly.io";
        String childName = "Aarav's account";

        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now().minusDays(30);
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        if (end.isBefore(start)) end = start.plusDays(30);
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        if (daysBetween <= 0) daysBetween = 1;

        int count = Math.max(1, Math.min(req.getCount(), 100));
        List<Transaction> created = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            LocalDate date = start.plusDays(random.nextLong(daysBetween + 1));
            String typeChoice = req.getTransactionType();
            boolean isIncome = "INCOME".equalsIgnoreCase(typeChoice) || ("BOTH".equalsIgnoreCase(typeChoice) && i % 6 == 0);

            if (isIncome) {
                String desc = DEMO_INCOME.get(random.nextInt(DEMO_INCOME.size()));
                BigDecimal amount = BigDecimal.valueOf(15000 + random.nextInt(35000)).setScale(2, RoundingMode.HALF_UP);
                Transaction tx = buildTransaction(date, "[DEMO] " + desc, "Salary & Income", amount,
                        TransactionDirection.CREDIT, TransactionType.INCOME, false, account);
                created.add(tx);
            } else {
                // Select category
                String cat = selectCategory(req.getCategories(), random);
                String desc = selectMerchant(cat, random);
                BigDecimal amount = generateAmount(cat, random);

                Transaction tx = buildTransaction(date, "[DEMO] " + desc, cat, amount,
                        TransactionDirection.DEBIT, TransactionType.EXPENSE, false, account);
                created.add(tx);
            }
        }

        // Optional: internal transfers
        if (req.isIncludeTransfers()) {
            LocalDate date = start.plusDays(random.nextLong(daysBetween + 1));
            String desc = DEMO_TRANSFERS.get(random.nextInt(DEMO_TRANSFERS.size()));
            BigDecimal amount = BigDecimal.valueOf(1000 + random.nextInt(4000)).setScale(2, RoundingMode.HALF_UP);
            Transaction trf = buildTransaction(date, "[DEMO] " + desc, "Transfers", amount,
                    TransactionDirection.DEBIT, TransactionType.TRANSFER, true, account);
            created.add(trf);
        }

        // Optional: duplicate row for deduplication testing
        if (req.isIncludeDuplicates() && !created.isEmpty()) {
            Transaction original = created.get(0);
            Transaction duplicate = buildTransaction(original.getTransactionDate(), original.getDescription(),
                    original.getCategory(), original.getAmount(), original.getDirection(), original.getType(),
                    original.isTransfer(), account);
            duplicate.setFingerprint(original.getFingerprint());
            created.add(duplicate);
        }

        // Save transactions
        transactionRepository.saveAll(created);

        // Optional: attempted restricted transactions
        if (req.isIncludeAttemptedRestricted()) {
            parentControlService.recordBlockedTransaction(
                    childName,
                    "[DEMO] Steam Winter Gaming Bundle",
                    new BigDecimal("1299.00"),
                    "Gaming / Apps",
                    "Simulated Demo Attempt: 'Gaming / Apps' category is restricted under parental controls"
            );
        }

        // Optional: budget overruns
        if (req.isIncludeBudgetOverruns()) {
            Transaction overrun = buildTransaction(LocalDate.now(), "[DEMO] Zara Designer Jacket (Budget Overrun)",
                    "Shopping", new BigDecimal("5450.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, account);
            transactionRepository.save(overrun);
        }

        // Optional: split bills and lending
        if (req.isIncludeSplitBillsAndLoans()) {
            seedSocialFinanceRecords();
        }

        long demoCount = transactionRepository.countByIsDemoDataTrue();
        long realCount = transactionRepository.countByIsDemoDataFalse();

        return new DemoDataResponse(
                true,
                "Successfully generated " + created.size() + " realistic demo transactions for " + req.getAccount() + " account.",
                created.size(),
                demoCount,
                realCount,
                req.getAccount()
        );
    }

    @Transactional
    public DemoDataResponse loadStandardSampleDataset() {
        // Reset old demo data first
        transactionRepository.deleteByIsDemoDataTrue();

        LocalDate now = LocalDate.now();
        List<Transaction> list = List.of(
                buildTransaction(now.minusDays(14), "[DEMO] Monthly Salary Credit", "Salary & Income", new BigDecimal("45000.00"), TransactionDirection.CREDIT, TransactionType.INCOME, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(13), "[DEMO] Blue Tokai Coffee Roasters", "Food & Dining", new BigDecimal("450.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(12), "[DEMO] Swiggy Gourmet Feast", "Food & Dining", new BigDecimal("820.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(11), "[DEMO] Zara Autumn Blazer", "Shopping", new BigDecimal("3450.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(10), "[DEMO] Uber Premier City Commute", "Travel & Transport", new BigDecimal("480.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(9), "[DEMO] Airtel Xstream Fiber Broadband", "Bills & Utilities", new BigDecimal("999.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(8), "[DEMO] Nature's Basket Organic Groceries", "Groceries", new BigDecimal("1850.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(7), "[DEMO] BookMyShow IMAX Cinema", "Entertainment", new BigDecimal("950.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(6), "[DEMO] Myntra Festive Collection", "Shopping", new BigDecimal("2100.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(5), "[DEMO] Starbucks Mocha Frappuccino", "Food & Dining", new BigDecimal("380.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(4), "[DEMO] Delhi Metro SmartCard Recharge", "Travel & Transport", new BigDecimal("500.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(3), "[DEMO] Tata Power Electricity Utility", "Bills & Utilities", new BigDecimal("1850.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(2), "[DEMO] Netflix Premium 4K UHD", "Entertainment", new BigDecimal("649.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now.minusDays(1), "[DEMO] Blinkit Quick Groceries", "Groceries", new BigDecimal("680.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE, false, "vrinda@spendly.io"),
                buildTransaction(now, "[DEMO] Transfer to Savings Deposit", "Transfers", new BigDecimal("5000.00"), TransactionDirection.DEBIT, TransactionType.TRANSFER, true, "vrinda@spendly.io")
        );

        transactionRepository.saveAll(list);
        seedSocialFinanceRecords();
        seedBudgets();

        long demoCount = transactionRepository.countByIsDemoDataTrue();
        long realCount = transactionRepository.countByIsDemoDataFalse();

        return new DemoDataResponse(
                true,
                "Standard reference dataset loaded successfully (" + list.size() + " transactions).",
                list.size(),
                demoCount,
                realCount,
                "Parent"
        );
    }

    @Transactional
    public DemoDataResponse resetDemoData() {
        long deletedCount = transactionRepository.deleteByIsDemoDataTrue();
        long realCount = transactionRepository.countByIsDemoDataFalse();

        return new DemoDataResponse(
                true,
                "Demo data reset complete. " + deletedCount + " demo transactions deleted. " + realCount + " real user records preserved intact.",
                (int) deletedCount,
                0,
                realCount,
                "All"
        );
    }

    public DemoDataResponse getDemoStatus() {
        long demoCount = transactionRepository.countByIsDemoDataTrue();
        long realCount = transactionRepository.countByIsDemoDataFalse();

        return new DemoDataResponse(
                true,
                "Demo status retrieved.",
                0,
                demoCount,
                realCount,
                "Active"
        );
    }

    private Transaction buildTransaction(LocalDate date, String desc, String cat, BigDecimal amount,
                                         TransactionDirection dir, TransactionType type, boolean isTransfer, String account) {
        Transaction tx = new Transaction();
        tx.setTransactionDate(date);
        tx.setDescription(desc);
        tx.setCategory(cat);
        tx.setAmount(amount);
        tx.setDirection(dir);
        tx.setType(type);
        tx.setTransfer(isTransfer);
        tx.setDemoData(true);
        tx.setUserEmail(account);
        tx.setReferenceNumber("DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        String fp = generateFingerprint(date, desc, amount, dir.name(), tx.getReferenceNumber());
        tx.setFingerprint(fp);

        return tx;
    }

    private String selectCategory(List<String> userCategories, Random random) {
        if (userCategories != null && !userCategories.isEmpty()) {
            return userCategories.get(random.nextInt(userCategories.size()));
        }
        List<String> pool = List.of("Food & Dining", "Shopping", "Travel & Transport", "Bills & Utilities", "Entertainment", "Groceries");
        return pool.get(random.nextInt(pool.size()));
    }

    private String selectMerchant(String category, Random random) {
        switch (category.toLowerCase()) {
            case "food & dining":
            case "food":
                return DEMO_FOOD.get(random.nextInt(DEMO_FOOD.size()));
            case "shopping":
                return DEMO_SHOPPING.get(random.nextInt(DEMO_SHOPPING.size()));
            case "travel & transport":
            case "transport":
                return DEMO_TRAVEL.get(random.nextInt(DEMO_TRAVEL.size()));
            case "bills & utilities":
            case "bills":
                return DEMO_BILLS.get(random.nextInt(DEMO_BILLS.size()));
            case "entertainment":
                return DEMO_ENTERTAINMENT.get(random.nextInt(DEMO_ENTERTAINMENT.size()));
            case "gaming / apps":
            case "gaming":
                return DEMO_GAMING.get(random.nextInt(DEMO_GAMING.size()));
            case "groceries":
                return DEMO_GROCERIES.get(random.nextInt(DEMO_GROCERIES.size()));
            default:
                return "Local Merchant Transaction";
        }
    }

    private BigDecimal generateAmount(String category, Random random) {
        int base;
        switch (category.toLowerCase()) {
            case "food & dining":
                base = 250 + random.nextInt(1200);
                break;
            case "shopping":
                base = 800 + random.nextInt(4500);
                break;
            case "travel & transport":
                base = 120 + random.nextInt(850);
                break;
            case "bills & utilities":
                base = 650 + random.nextInt(2500);
                break;
            case "entertainment":
                base = 350 + random.nextInt(1800);
                break;
            default:
                base = 200 + random.nextInt(1000);
        }
        return BigDecimal.valueOf(base).setScale(2, RoundingMode.HALF_UP);
    }

    private void seedSocialFinanceRecords() {
        if (splitBillRepository.count() == 0) {
            SplitBill goa = new SplitBill("Goa Trip Villa & Beach Shack", new BigDecimal("9000.00"), LocalDate.now(), "Equally split between 3");
            goa.addParticipant(new SplitBillParticipant("Vrinda (You)", new BigDecimal("3000.00"), true));
            goa.addParticipant(new SplitBillParticipant("Rahul", new BigDecimal("3000.00"), false));
            goa.addParticipant(new SplitBillParticipant("Aadya", new BigDecimal("3000.00"), false));
            splitBillRepository.save(goa);

            SplitBill flat = new SplitBill("Flatmates Groceries & Wifi", new BigDecimal("3600.00"), LocalDate.now(), "October shared maintenance");
            flat.addParticipant(new SplitBillParticipant("Vrinda (You)", new BigDecimal("1200.00"), true));
            flat.addParticipant(new SplitBillParticipant("Riya", new BigDecimal("1200.00"), true));
            flat.addParticipant(new SplitBillParticipant("Om", new BigDecimal("1200.00"), false));
            splitBillRepository.save(flat);
        }

        if (lendingObligationRepository.count() == 0) {
            lendingObligationRepository.save(new LendingObligation("Rahul", ObligationType.LENT, new BigDecimal("2000.00"), LocalDate.now(), LocalDate.now().plusDays(10), "Dinner contribution"));
            lendingObligationRepository.save(new LendingObligation("Aadya", ObligationType.LENT, new BigDecimal("1250.00"), LocalDate.now(), LocalDate.now().plusDays(14), "Concert tickets"));
            lendingObligationRepository.save(new LendingObligation("Riya", ObligationType.BORROWED, new BigDecimal("720.00"), LocalDate.now(), LocalDate.now().plusDays(5), "Uber ride share"));
            lendingObligationRepository.save(new LendingObligation("Om", ObligationType.BORROWED, new BigDecimal("600.00"), LocalDate.now(), LocalDate.now().plusDays(7), "Textbook photocopy"));
        }
    }

    private void seedBudgets() {
        if (budgetRepository.count() == 0) {
            String ym = LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
            budgetRepository.save(new Budget("Food & Dining", ym, new BigDecimal("8000.00")));
            budgetRepository.save(new Budget("Shopping", ym, new BigDecimal("10000.00")));
            budgetRepository.save(new Budget("Travel & Transport", ym, new BigDecimal("4000.00")));
            budgetRepository.save(new Budget("Bills & Utilities", ym, new BigDecimal("5000.00")));
            budgetRepository.save(new Budget("Entertainment", ym, new BigDecimal("4000.00")));
            budgetRepository.save(new Budget("Groceries", ym, new BigDecimal("6000.00")));
        }
    }

    private String generateFingerprint(LocalDate date, String desc, BigDecimal amt, String dir, String ref) {
        try {
            String raw = date.toString() + "|" + desc.toLowerCase().trim() + "|" + amt.setScale(2, RoundingMode.HALF_UP) + "|" + dir + "|" + (ref != null ? ref.trim() : "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
