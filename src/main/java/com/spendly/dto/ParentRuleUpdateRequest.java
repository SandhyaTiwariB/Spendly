package com.spendly.dto;

import java.math.BigDecimal;

public class ParentRuleUpdateRequest {

    private Boolean enabled;
    private BigDecimal categoryLimit;

    public ParentRuleUpdateRequest() {}

    public ParentRuleUpdateRequest(Boolean enabled, BigDecimal categoryLimit) {
        this.enabled = enabled;
        this.categoryLimit = categoryLimit;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getIsAllowed() {
        return enabled;
    }

    public void setIsAllowed(Boolean isAllowed) {
        this.enabled = isAllowed;
    }

    public BigDecimal getCategoryLimit() {
        return categoryLimit;
    }

    public void setCategoryLimit(BigDecimal categoryLimit) {
        this.categoryLimit = categoryLimit;
    }
}
