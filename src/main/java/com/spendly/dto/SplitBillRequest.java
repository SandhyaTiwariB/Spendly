package com.spendly.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SplitBillRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal totalAmount;

    private LocalDate billDate;

    private String notes;

    private List<ParticipantItem> participants;

    public static class ParticipantItem {
        private String name;
        private BigDecimal shareAmount;
        private boolean isSettled;

        public ParticipantItem() {}

        @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING)
        public ParticipantItem(String name) {
            this.name = name;
            this.shareAmount = null;
            this.isSettled = false;
        }

        public ParticipantItem(String name, BigDecimal shareAmount, boolean isSettled) {
            this.name = name;
            this.shareAmount = shareAmount;
            this.isSettled = isSettled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getShareAmount() {
            return shareAmount;
        }

        public void setShareAmount(BigDecimal shareAmount) {
            this.shareAmount = shareAmount;
        }

        public boolean isSettled() {
            return isSettled;
        }

        public void setSettled(boolean settled) {
            isSettled = settled;
        }
    }

    public SplitBillRequest() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDate billDate) {
        this.billDate = billDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ParticipantItem> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ParticipantItem> participants) {
        this.participants = participants;
    }
}
