package com.spendly.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parent_activity_logs", indexes = {
    @Index(name = "idx_act_timestamp", columnList = "timestamp"),
    @Index(name = "idx_act_child", columnList = "childAccount"),
    @Index(name = "idx_act_type", columnList = "activityType"),
    @Index(name = "idx_act_status", columnList = "status")
})
public class ParentActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(nullable = false, length = 100)
    private String childAccount = "Aarav's account";

    @Column(nullable = false, length = 80)
    private String activityType;

    @Column(length = 100)
    private String category;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 200)
    private String previousValue;

    @Column(length = 200)
    private String newValue;

    @Column(nullable = false, length = 50)
    private String status = "COMPLETED";

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, length = 100)
    private String source = "Spendly Application";

    @Column(nullable = false, length = 100)
    private String actor = "Parent (Vrinda)";

    public ParentActivityLog() {}

    public ParentActivityLog(String childAccount, String activityType, String category, BigDecimal amount,
                             String description, String previousValue, String newValue, String status,
                             String reason, String source, String actor) {
        this.timestamp = LocalDateTime.now();
        this.childAccount = childAccount != null ? childAccount : "Aarav's account";
        this.activityType = activityType;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.status = status != null ? status : "COMPLETED";
        this.reason = reason;
        this.source = source != null ? source : "Spendly Application";
        this.actor = actor != null ? actor : "Parent (Vrinda)";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getChildAccount() {
        return childAccount;
    }

    public void setChildAccount(String childAccount) {
        this.childAccount = childAccount;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }
}
