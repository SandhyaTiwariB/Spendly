package com.spendly.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lending_obligations")
public class LendingObligation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String personName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObligationType type = ObligationType.LENT;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private LocalDate startDate = LocalDate.now();

    private LocalDate dueDate;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObligationStatus status = ObligationStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "obligation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<ObligationRepayment> repayments = new ArrayList<>();

    public LendingObligation() {}

    public LendingObligation(String personName, ObligationType type, BigDecimal amount, LocalDate startDate, LocalDate dueDate, String notes) {
        this.personName = personName;
        this.type = type;
        this.amount = amount;
        this.balance = amount;
        this.startDate = startDate != null ? startDate : LocalDate.now();
        this.dueDate = dueDate;
        this.notes = notes;
        this.status = ObligationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void addRepayment(ObligationRepayment repayment) {
        repayments.add(repayment);
        repayment.setObligation(this);
        this.balance = this.balance.subtract(repayment.getAmount());
        if (this.balance.compareTo(BigDecimal.ZERO) <= 0) {
            this.balance = BigDecimal.ZERO;
            this.status = ObligationStatus.SETTLED;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public ObligationType getType() {
        return type;
    }

    public void setType(ObligationType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public ObligationStatus getStatus() {
        return status;
    }

    public void setStatus(ObligationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ObligationRepayment> getRepayments() {
        return repayments;
    }

    public void setRepayments(List<ObligationRepayment> repayments) {
        this.repayments = repayments;
    }
}
