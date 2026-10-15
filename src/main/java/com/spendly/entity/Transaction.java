package com.spendly.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_tx_date", columnList = "transactionDate"),
    @Index(name = "idx_tx_fingerprint", columnList = "fingerprint"),
    @Index(name = "idx_tx_category", columnList = "category"),
    @Index(name = "idx_tx_type", columnList = "type"),
    @Index(name = "idx_tx_import_id", columnList = "importReference")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long importReference;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private LocalDate valueDate;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 255)
    private String referenceNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionDirection direction = TransactionDirection.DEBIT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type = TransactionType.EXPENSE;

    @Column(nullable = false, length = 100)
    private String category = "Other";

    @Column(nullable = false)
    private boolean isTransfer = false;

    @Column(precision = 15, scale = 2)
    private BigDecimal runningBalance;

    @Column(length = 64)
    private String fingerprint;

    @Column(nullable = false)
    private boolean isManualCategory = false;

    @Column(nullable = false)
    private boolean isDemoData = false;

    @Column(length = 150)
    private String userEmail = "vrinda@spendly.io";

    @Column(columnDefinition = "TEXT")
    private String rawRowData;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Transaction() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getImportReference() {
        return importReference;
    }

    public void setImportReference(Long importReference) {
        this.importReference = importReference;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null && description.length() > 1950) {
            this.description = description.substring(0, 1950);
        } else {
            this.description = description;
        }
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        if (referenceNumber != null && referenceNumber.length() > 250) {
            this.referenceNumber = referenceNumber.substring(0, 250);
        } else {
            this.referenceNumber = referenceNumber;
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionDirection getDirection() {
        return direction;
    }

    public void setDirection(TransactionDirection direction) {
        this.direction = direction;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getRawRowData() {
        return rawRowData;
    }

    public void setRawRowData(String rawRowData) {
        this.rawRowData = rawRowData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
