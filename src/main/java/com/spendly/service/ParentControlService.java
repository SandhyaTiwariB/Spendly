package com.spendly.service;

import com.spendly.dto.ParentRuleUpdateRequest;
import com.spendly.dto.ParentSummaryResponse;
import com.spendly.entity.ParentActivityLog;
import com.spendly.entity.ParentAlert;
import com.spendly.entity.ParentRule;
import com.spendly.repository.ParentActivityLogRepository;
import com.spendly.repository.ParentAlertRepository;
import com.spendly.repository.ParentRuleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParentControlService {

    private final ParentRuleRepository ruleRepository;
    private final ParentActivityLogRepository logRepository;
    private final ParentAlertRepository alertRepository;

    private static final String DEFAULT_CHILD = "Aarav's account";

    public ParentControlService(ParentRuleRepository ruleRepository,
                                ParentActivityLogRepository logRepository,
                                ParentAlertRepository alertRepository) {
        this.ruleRepository = ruleRepository;
        this.logRepository = logRepository;
        this.alertRepository = alertRepository;
    }

    @PostConstruct
    public void initDefaultRulesAndLogs() {
        if (ruleRepository.count() == 0) {
            // Category controls matching screenshot & live website
            ruleRepository.save(new ParentRule(DEFAULT_CHILD, "Shopping", true, new BigDecimal("4000.00")));
            ruleRepository.save(new ParentRule(DEFAULT_CHILD, "Entertainment", true, new BigDecimal("3000.00")));
            ruleRepository.save(new ParentRule(DEFAULT_CHILD, "Food & Dining", true, new BigDecimal("3500.00")));
            ruleRepository.save(new ParentRule(DEFAULT_CHILD, "Gaming / Apps", false, new BigDecimal("0.00")));
        }

        if (logRepository.count() < 15) {
            if (logRepository.count() == 0) {
                ParentActivityLog l1 = new ParentActivityLog(
                        DEFAULT_CHILD,
                        "CATEGORY_PERMISSION_CHANGED",
                        "Gaming / Apps",
                        null,
                        "Gaming / Apps spending permission was locked by parent",
                        "Enabled",
                        "Disabled",
                        "COMPLETED",
                        "Parent restricted in-game purchases and app store downloads",
                        "Spendly Application",
                        "Parent (Vrinda)"
                );
                l1.setTimestamp(LocalDateTime.now().minusDays(3).minusHours(2));
                logRepository.save(l1);

                ParentActivityLog l2 = new ParentActivityLog(
                        DEFAULT_CHILD,
                        "MONTHLY_LIMIT_CHANGED",
                        "All",
                        new BigDecimal("12000.00"),
                        "Monthly spending limit set to ₹12,000.00",
                        "₹10,000.00",
                        "₹12,000.00",
                        "COMPLETED",
                        "Revised monthly allowance for school semester",
                        "Spendly Application",
                        "Parent (Vrinda)"
                );
                l2.setTimestamp(LocalDateTime.now().minusDays(2).minusHours(5));
                logRepository.save(l2);

                ParentActivityLog l3 = new ParentActivityLog(
                        DEFAULT_CHILD,
                        "TRANSACTION_BLOCKED",
                        "Gaming / Apps",
                        new BigDecimal("699.00"),
                        "Blocked transaction attempt: Play Store Gaming Bundle",
                        null,
                        null,
                        "BLOCKED",
                        "Category 'Gaming / Apps' is currently restricted under Parent Control rules",
                        "Spendly Application",
                        "Aarav"
                );
                l3.setTimestamp(LocalDateTime.now().minusDays(1).minusHours(7));
                logRepository.save(l3);

                ParentActivityLog l4 = new ParentActivityLog(
                        DEFAULT_CHILD,
                        "TRANSACTION_FLAGGED",
                        "Gaming / Apps",
                        new BigDecimal("1499.00"),
                        "Flagged imported bank statement debit: Steam Games",
                        null,
                        null,
                        "FLAGGED",
                        "Imported debit occurred outside Spendly in restricted category; flagged for parental review",
                        "Imported Bank Statement",
                        "Aarav"
                );
                l4.setTimestamp(LocalDateTime.now().minusDays(1).minusHours(4));
                logRepository.save(l4);

                ParentActivityLog l5 = new ParentActivityLog(
                        DEFAULT_CHILD,
                        "ALERT_GENERATED",
                        "Shopping",
                        new BigDecimal("3600.00"),
                        "Category limit warning: Shopping spent 90% of threshold",
                        null,
                        null,
                        "WARNING",
                        "Spending reached ₹3,600 of ₹4,000 category limit",
                        "Spendly Application",
                        "System Monitor"
                );
                l5.setTimestamp(LocalDateTime.now().minusHours(12));
                logRepository.save(l5);
            }

            // Additional events so pagination is active with multiple pages
            ParentActivityLog l6 = new ParentActivityLog(
                    DEFAULT_CHILD, "ALLOWANCE_CREDITED", "Food & Dining", new BigDecimal("850.00"),
                    "Approved weekend dining allowance request", null, "₹850.00", "COMPLETED",
                    "Allowance requested for group study lunch", "Spendly Application", "Parent (Vrinda)"
            );
            l6.setTimestamp(LocalDateTime.now().minusHours(10));
            logRepository.save(l6);

            ParentActivityLog l7 = new ParentActivityLog(
                    DEFAULT_CHILD, "ALERT_GENERATED", "Entertainment", new BigDecimal("2400.00"),
                    "Entertainment allowance warning: 80% threshold reached", null, null, "WARNING",
                    "Spending reached ₹2,400 of ₹3,000 limit", "Spendly Application", "System Monitor"
            );
            l7.setTimestamp(LocalDateTime.now().minusHours(8));
            logRepository.save(l7);

            ParentActivityLog l8 = new ParentActivityLog(
                    DEFAULT_CHILD, "TRANSACTION_BLOCKED", "Gaming / Apps", new BigDecimal("499.00"),
                    "Blocked late-night in-app subscription", null, null, "BLOCKED",
                    "Restricted category 'Gaming / Apps' enforced", "Spendly Application", "Aarav"
            );
            l8.setTimestamp(LocalDateTime.now().minusHours(7));
            logRepository.save(l8);

            ParentActivityLog l9 = new ParentActivityLog(
                    DEFAULT_CHILD, "PURCHASE_VERIFIED", "Food & Dining", new BigDecimal("340.00"),
                    "Campus cafeteria lunch payment recorded", null, null, "COMPLETED",
                    "Verified on-campus transaction", "Spendly Application", "Aarav"
            );
            l9.setTimestamp(LocalDateTime.now().minusHours(6));
            logRepository.save(l9);

            ParentActivityLog l10 = new ParentActivityLog(
                    DEFAULT_CHILD, "CATEGORY_LIMIT_CHANGED", "Entertainment", new BigDecimal("3000.00"),
                    "Monthly category limit revised for Entertainment", "₹2,500.00", "₹3,000.00", "COMPLETED",
                    "Adjusted for vacation activities", "Spendly Application", "Parent (Vrinda)"
            );
            l10.setTimestamp(LocalDateTime.now().minusHours(5));
            logRepository.save(l10);

            ParentActivityLog l11 = new ParentActivityLog(
                    DEFAULT_CHILD, "TRANSACTION_FLAGGED", "Food & Dining", new BigDecimal("620.00"),
                    "Flagged duplicate food delivery transaction", null, null, "FLAGGED",
                    "Potential duplicate charge within 10 minutes", "Imported Bank Statement", "Aarav"
            );
            l11.setTimestamp(LocalDateTime.now().minusHours(4));
            logRepository.save(l11);

            ParentActivityLog l12 = new ParentActivityLog(
                    DEFAULT_CHILD, "PURCHASE_VERIFIED", "Shopping", new BigDecimal("1800.00"),
                    "Bookstore study material purchase verified", null, null, "COMPLETED",
                    "Academic books purchase", "Spendly Application", "Aarav"
            );
            l12.setTimestamp(LocalDateTime.now().minusHours(3));
            logRepository.save(l12);

            ParentActivityLog l13 = new ParentActivityLog(
                    DEFAULT_CHILD, "TRANSACTION_BLOCKED", "Gaming / Apps", new BigDecimal("1299.00"),
                    "Blocked unverified gaming store transaction attempt", null, null, "BLOCKED",
                    "Locked under Parent Control settings", "Spendly Application", "Aarav"
            );
            l13.setTimestamp(LocalDateTime.now().minusHours(2));
            logRepository.save(l13);

            ParentActivityLog l14 = new ParentActivityLog(
                    DEFAULT_CHILD, "ALERT_GENERATED", "Shopping", new BigDecimal("3950.00"),
                    "Critical Alert: Shopping reached 98% of monthly cap", null, null, "WARNING",
                    "Only ₹50.00 remaining in monthly shopping limit", "Spendly Application", "System Monitor"
            );
            l14.setTimestamp(LocalDateTime.now().minusMinutes(45));
            logRepository.save(l14);

            ParentActivityLog l15 = new ParentActivityLog(
                    DEFAULT_CHILD, "ALLOWANCE_CREDITED", "General", new BigDecimal("500.00"),
                    "Pocket money top-up for weekend project", null, null, "COMPLETED",
                    "Bonus allowance approved by parent", "Spendly Application", "Parent (Vrinda)"
            );
            l15.setTimestamp(LocalDateTime.now().minusMinutes(15));
            logRepository.save(l15);
        }

        if (alertRepository.count() == 0) {
            alertRepository.save(new ParentAlert(
                    "Restricted Category Attempt Blocked",
                    "Spendly prevented an in-app transaction of ₹699.00 for 'Play Store Gaming Bundle' because Gaming / Apps is disabled.",
                    "DANGER",
                    "Gaming / Apps",
                    true
            ));

            alertRepository.save(new ParentAlert(
                    "Imported Statement Flag: Gaming / Apps",
                    "An imported bank statement contained a ₹1,499.00 debit from 'Steam Games'. Real-world card transactions cannot be stopped retroactively, so this record has been flagged for your review.",
                    "WARNING",
                    "Gaming / Apps",
                    false
            ));
        }
    }

    public ParentSummaryResponse getSummary(String childAccount) {
        String child = childAccount != null && !childAccount.trim().isEmpty() ? childAccount.trim() : DEFAULT_CHILD;

        ParentSummaryResponse response = new ParentSummaryResponse();
        response.setChildAccount(child);
        response.setChildAvatar("AR");
        response.setStatusText("Monitoring active");
        response.setActive(true);

        BigDecimal monthlyLimit = new BigDecimal("12000.00");
        BigDecimal spentThisMonth = new BigDecimal("8240.00");
        BigDecimal remaining = monthlyLimit.subtract(spentThisMonth);

        response.setMonthlyLimit(monthlyLimit);
        response.setSpentThisMonth(spentThisMonth);
        response.setRemainingLimit(remaining.max(BigDecimal.ZERO));
        response.setPercentageUsed(spentThisMonth.divide(monthlyLimit, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue());

        response.setCategoryRules(ruleRepository.findByChildAccount(child));
        response.setRecentActivity(logRepository.findFilteredLogs(child, null, null, null, null, null, null, PageRequest.of(0, 5)).getContent());
        response.setActiveAlerts(alertRepository.findByIsReadFalseOrderByTimestampDesc());

        return response;
    }

    public List<ParentRule> getRules(String childAccount) {
        String child = childAccount != null && !childAccount.trim().isEmpty() ? childAccount.trim() : DEFAULT_CHILD;
        return ruleRepository.findByChildAccount(child);
    }

    @Transactional
    public ParentRule updateRule(String childAccount, String category, ParentRuleUpdateRequest request) {
        String child = childAccount != null && !childAccount.trim().isEmpty() ? childAccount.trim() : DEFAULT_CHILD;

        ParentRule rule = ruleRepository.findByChildAccountAndCategoryIgnoreCase(child, category)
                .orElseGet(() -> new ParentRule(child, category, true, new BigDecimal("5000.00")));

        boolean previousState = rule.isEnabled();
        BigDecimal previousLimit = rule.getCategoryLimit();

        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }
        if (request.getCategoryLimit() != null) {
            rule.setCategoryLimit(request.getCategoryLimit());
        }

        ParentRule saved = ruleRepository.save(rule);

        // Record audit event
        String prevVal = previousState ? "Enabled" : "Disabled";
        String newVal = saved.isEnabled() ? "Enabled" : "Disabled";
        String description = "Category '" + category + "' permission switched from " + prevVal + " to " + newVal;

        logRepository.save(new ParentActivityLog(
                child,
                "CATEGORY_PERMISSION_CHANGED",
                category,
                saved.getCategoryLimit(),
                description,
                prevVal,
                newVal,
                "COMPLETED",
                "Parent modified category permission via Spendly Parent Control",
                "Spendly Application",
                "Parent (Vrinda)"
        ));

        return saved;
    }

    public Page<ParentActivityLog> getActivityLogs(String childAccount, String activityType, String category,
                                                   String status, LocalDateTime startDate, LocalDateTime endDate,
                                                   String search, Pageable pageable) {
        return logRepository.findFilteredLogs(childAccount, activityType, category, status, startDate, endDate, search, pageable);
    }

    public List<ParentAlert> getAlerts() {
        return alertRepository.findAllByOrderByTimestampDesc();
    }

    @Transactional
    public void markAlertRead(Long id) {
        alertRepository.findById(id).ifPresent(a -> {
            a.setRead(true);
            alertRepository.save(a);
        });
    }

    public boolean isCategoryAllowed(String childAccount, String category) {
        if (category == null) return true;
        String child = childAccount != null ? childAccount : DEFAULT_CHILD;
        return ruleRepository.findByChildAccountAndCategoryIgnoreCase(child, category)
                .map(ParentRule::isEnabled)
                .orElse(true);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordBlockedTransaction(String childAccount, String description, BigDecimal amount, String category, String reason) {
        String child = childAccount != null ? childAccount : DEFAULT_CHILD;

        logRepository.save(new ParentActivityLog(
                child,
                "TRANSACTION_BLOCKED",
                category,
                amount,
                "Blocked transaction attempt: " + description,
                null,
                null,
                "BLOCKED",
                reason,
                "Spendly Application",
                child
        ));

        alertRepository.save(new ParentAlert(
                "Restricted Category Attempt Blocked",
                "Spendly blocked a transaction of ₹" + amount + " for '" + description + "' in restricted category '" + category + "'.",
                "DANGER",
                category,
                true
        ));
    }

    @Transactional
    public void flagImportedRestrictedTransaction(String description, BigDecimal amount, String category) {
        logRepository.save(new ParentActivityLog(
                DEFAULT_CHILD,
                "TRANSACTION_FLAGGED",
                category,
                amount,
                "Flagged imported statement transaction: " + description,
                null,
                null,
                "FLAGGED",
                "Imported bank statement transaction occurred outside Spendly in restricted category '" + category + "'; flagged for parental review",
                "Imported Bank Statement",
                DEFAULT_CHILD
        ));

        alertRepository.save(new ParentAlert(
                "Imported Statement Flag: " + category,
                "An imported bank statement recorded a ₹" + amount + " transaction for '" + description + "'. External card/bank payments cannot be blocked retroactively and require parental review.",
                "WARNING",
                category,
                false
        ));
    }

    @Transactional
    public void registerNewCategoryRule(String category) {
        if (category == null || category.trim().isEmpty()) return;
        String cleanCat = category.trim();
        if (ruleRepository.findByChildAccountAndCategoryIgnoreCase(DEFAULT_CHILD, cleanCat).isEmpty()) {
            ruleRepository.save(new ParentRule(DEFAULT_CHILD, cleanCat, true, new BigDecimal("5000.00")));
            logRepository.save(new ParentActivityLog(
                    DEFAULT_CHILD,
                    "CATEGORY_PERMISSION_CHANGED",
                    cleanCat,
                    new BigDecimal("5000.00"),
                    "New category '" + cleanCat + "' created and configured under Parent Control",
                    null,
                    "Enabled",
                    "COMPLETED",
                    "Parent created category and assigned default limit of ₹5,000.00",
                    "Spendly Application",
                    "Parent (Vrinda)"
            ));
        }
    }

    @Transactional
    public void removeCategoryRule(String category) {
        if (category == null || category.trim().isEmpty()) return;
        String cleanCat = category.trim();
        ruleRepository.findByChildAccountAndCategoryIgnoreCase(DEFAULT_CHILD, cleanCat)
                .ifPresent(rule -> {
                    ruleRepository.delete(rule);
                    logRepository.save(new ParentActivityLog(
                            DEFAULT_CHILD,
                            "CATEGORY_PERMISSION_CHANGED",
                            cleanCat,
                            null,
                            "Category '" + cleanCat + "' was deleted and removed from Parent Control rules",
                            "Active",
                            "Removed",
                            "COMPLETED",
                            "Parent deleted custom category",
                            "Spendly Application",
                            "Parent (Vrinda)"
                    ));
                });
    }
}
