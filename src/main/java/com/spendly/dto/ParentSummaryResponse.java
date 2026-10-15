package com.spendly.dto;

import com.spendly.entity.ParentActivityLog;
import com.spendly.entity.ParentAlert;
import com.spendly.entity.ParentRule;
import java.math.BigDecimal;
import java.util.List;

public class ParentSummaryResponse {

    private String childAccount = "Aarav's account";
    private String childAvatar = "AR";
    private String statusText = "Monitoring active";
    private boolean active = true;
    private BigDecimal monthlyLimit = new BigDecimal("12000.00");
    private BigDecimal spentThisMonth = new BigDecimal("8240.00");
    private BigDecimal remainingLimit = new BigDecimal("3760.00");
    private double percentageUsed = 68.67;
    private List<ParentRule> categoryRules;
    private List<ParentActivityLog> recentActivity;
    private List<ParentAlert> activeAlerts;

    public ParentSummaryResponse() {}

    public String getChildAccount() {
        return childAccount;
    }

    public void setChildAccount(String childAccount) {
        this.childAccount = childAccount;
    }

    public String getChildAvatar() {
        return childAvatar;
    }

    public void setChildAvatar(String childAvatar) {
        this.childAvatar = childAvatar;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public BigDecimal getSpentThisMonth() {
        return spentThisMonth;
    }

    public void setSpentThisMonth(BigDecimal spentThisMonth) {
        this.spentThisMonth = spentThisMonth;
    }

    public BigDecimal getRemainingLimit() {
        return remainingLimit;
    }

    public void setRemainingLimit(BigDecimal remainingLimit) {
        this.remainingLimit = remainingLimit;
    }

    public double getPercentageUsed() {
        return percentageUsed;
    }

    public void setPercentageUsed(double percentageUsed) {
        this.percentageUsed = percentageUsed;
    }

    public List<ParentRule> getCategoryRules() {
        return categoryRules;
    }

    public void setCategoryRules(List<ParentRule> categoryRules) {
        this.categoryRules = categoryRules;
    }

    public List<ParentActivityLog> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<ParentActivityLog> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public List<ParentAlert> getActiveAlerts() {
        return activeAlerts;
    }

    public void setActiveAlerts(List<ParentAlert> activeAlerts) {
        this.activeAlerts = activeAlerts;
    }
}
