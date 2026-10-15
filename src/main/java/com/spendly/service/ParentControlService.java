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

        if (logRepository.count() == 0) {
            logRepository.save(new ParentActivityLog(
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
            ));

            logRepository.save(new ParentActivityLog(
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
            ));

            logRepository.save(new ParentActivityLog(
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
            ));

            logRepository.save(new ParentActivityLog(
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
            ));

            logRepository.save(new ParentActivityLog(
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
            ));
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
