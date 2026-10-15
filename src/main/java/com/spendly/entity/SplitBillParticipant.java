package com.spendly.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "split_bill_participants")
public class SplitBillParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_bill_id", nullable = false)
    @JsonBackReference
    private SplitBill splitBill;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal shareAmount;

    @Column(nullable = false)
    private boolean isSettled = false;

    private LocalDate settledDate;

    public SplitBillParticipant() {}

    public SplitBillParticipant(String name, BigDecimal shareAmount, boolean isSettled) {
        this.name = name;
        this.shareAmount = shareAmount;
        this.isSettled = isSettled;
        if (isSettled) {
            this.settledDate = LocalDate.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SplitBill getSplitBill() {
        return splitBill;
    }

    public void setSplitBill(SplitBill splitBill) {
        this.splitBill = splitBill;
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
        if (settled && this.settledDate == null) {
            this.settledDate = LocalDate.now();
        } else if (!settled) {
            this.settledDate = null;
        }
    }

    public LocalDate getSettledDate() {
        return settledDate;
    }

    public void setSettledDate(LocalDate settledDate) {
        this.settledDate = settledDate;
    }
}
