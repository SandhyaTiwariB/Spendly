package com.spendly.dto;

import com.spendly.entity.ObligationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LendingRequest {

    @NotBlank(message = "Person name is required")
    private String personName;

    @NotNull(message = "Obligation type (LENT or BORROWED) is required")
    private ObligationType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private LocalDate startDate;
    private LocalDate dueDate;
    private String notes;

    public LendingRequest() {}

    public LendingRequest(String personName, ObligationType type, BigDecimal amount, LocalDate startDate, LocalDate dueDate, String notes) {
        this.personName = personName;
        this.type = type;
        this.amount = amount;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.notes = notes;
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
}
