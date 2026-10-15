package com.spendly.dto;

public class TransactionPatchRequest {
    private String category;
    private String description;
    private Boolean isTransfer;

    public TransactionPatchRequest() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsTransfer() {
        return isTransfer;
    }

    public void setIsTransfer(Boolean transfer) {
        isTransfer = transfer;
    }
}
