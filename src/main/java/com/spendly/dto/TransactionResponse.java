package com.spendly.dto;

import com.spendly.entity.Transaction;
import com.spendly.entity.TransactionDirection;
import com.spendly.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionResponse {
    private Long id;
    private LocalDate date;
    private String description;
    private String category;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionDirection direction;
    private boolean isTransfer;
    private BigDecimal runningBalance;
    private String referenceNumber;
    private boolean isManualCategory;
    private boolean isDemoData;

    public TransactionResponse() {
    }

    public static TransactionResponse fromEntity(Transaction t) {
        TransactionResponse r = new TransactionResponse();
        r.setId(t.getId());
        r.setDate(t.getTransactionDate());
        r.setDescription(t.getDescription());
        r.setCategory(t.getCategory());
        r.setAmount(t.getAmount());
        r.setType(t.getType());
        r.setDirection(t.getDirection());
        r.setTransfer(t.isTransfer());
        r.setRunningBalance(t.getRunningBalance());
        r.setReferenceNumber(t.getReferenceNumber());
        r.setManualCategory(t.isManualCategory());
        r.setDemoData(t.isDemoData());
        return r;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public String getMerchant() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionDirection getDirection() {
        return direction;
    }

    public void setDirection(TransactionDirection direction) {
        this.direction = direction;
    }

    public boolean isTransfer() {
        return isTransfer;
    }

    public void setTransfer(boolean transfer) {
        isTransfer = transfer;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public boolean isManualCategory() {
        return isManualCategory;
    }

    public void setManualCategory(boolean manualCategory) {
        isManualCategory = manualCategory;
    }

    public boolean isDemoData() {
        return isDemoData;
    }

    public void setDemoData(boolean demoData) {
        isDemoData = demoData;
    }
}
