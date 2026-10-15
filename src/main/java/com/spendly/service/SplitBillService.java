package com.spendly.service;

import com.spendly.dto.SplitBillRequest;
import com.spendly.entity.SplitBill;
import com.spendly.entity.SplitBillParticipant;
import com.spendly.repository.SplitBillParticipantRepository;
import com.spendly.repository.SplitBillRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class SplitBillService {

    private final SplitBillRepository splitBillRepository;
    private final SplitBillParticipantRepository participantRepository;

    public SplitBillService(SplitBillRepository splitBillRepository,
                            SplitBillParticipantRepository participantRepository) {
        this.splitBillRepository = splitBillRepository;
        this.participantRepository = participantRepository;
    }

    @PostConstruct
    public void initDefaultSplitBills() {
        if (splitBillRepository.count() == 0) {
            // Group 1: Goa Trip (+₹2,400)
            SplitBill goa = new SplitBill("Goa Trip", new BigDecimal("9600.00"), LocalDate.now().minusDays(5), "Villa stay & scooters");
            goa.addParticipant(new SplitBillParticipant("Rahul", new BigDecimal("2400.00"), false));
            goa.addParticipant(new SplitBillParticipant("Aadya", new BigDecimal("2400.00"), false));
            goa.addParticipant(new SplitBillParticipant("Om", new BigDecimal("2400.00"), true));
            goa.addParticipant(new SplitBillParticipant("Vrinda", new BigDecimal("2400.00"), true));
            splitBillRepository.save(goa);

            // Group 2: Flatmates (-₹780)
            SplitBill flat = new SplitBill("Flatmates", new BigDecimal("3120.00"), LocalDate.now().minusDays(2), "Groceries and wifi bill");
            flat.addParticipant(new SplitBillParticipant("Riya", new BigDecimal("1040.00"), true));
            flat.addParticipant(new SplitBillParticipant("Aman", new BigDecimal("1040.00"), true));
            flat.addParticipant(new SplitBillParticipant("Vrinda (You)", new BigDecimal("1040.00"), false));
            splitBillRepository.save(flat);

            // Group 3: Besties (+₹1,050)
            SplitBill besties = new SplitBill("Besties", new BigDecimal("3500.00"), LocalDate.now().minusDays(8), "Weekend cafe meetup");
            besties.addParticipant(new SplitBillParticipant("Sneha", new BigDecimal("700.00"), false));
            besties.addParticipant(new SplitBillParticipant("Kunal", new BigDecimal("700.00"), false));
            besties.addParticipant(new SplitBillParticipant("Rohit", new BigDecimal("700.00"), true));
            besties.addParticipant(new SplitBillParticipant("Neha", new BigDecimal("700.00"), true));
            besties.addParticipant(new SplitBillParticipant("Vrinda", new BigDecimal("700.00"), true));
            splitBillRepository.save(besties);
        }
    }

    public List<SplitBill> getAllBills() {
        return splitBillRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<SplitBill> getBillById(Long id) {
        return splitBillRepository.findById(id);
    }

    @Transactional
    public SplitBill createBill(SplitBillRequest request) {
        SplitBill bill = new SplitBill(
                request.getTitle().trim(),
                request.getTotalAmount(),
                request.getBillDate() != null ? request.getBillDate() : LocalDate.now(),
                request.getNotes()
        );

        if (request.getParticipants() != null && !request.getParticipants().isEmpty()) {
            int count = request.getParticipants().size();
            BigDecimal equalShare = (request.getTotalAmount() != null && count > 0)
                    ? request.getTotalAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            for (SplitBillRequest.ParticipantItem item : request.getParticipants()) {
                BigDecimal share = (item.getShareAmount() != null && item.getShareAmount().compareTo(BigDecimal.ZERO) > 0)
                        ? item.getShareAmount()
                        : equalShare;
                bill.addParticipant(new SplitBillParticipant(
                        item.getName() != null ? item.getName().trim() : "Participant",
                        share,
                        item.isSettled()
                ));
            }
        } else {
            // Default 2-way split
            BigDecimal half = request.getTotalAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            bill.addParticipant(new SplitBillParticipant("Participant 1", half, false));
            bill.addParticipant(new SplitBillParticipant("Vrinda (You)", half, true));
        }

        return splitBillRepository.save(bill);
    }

    @Transactional
    public SplitBill settleParticipant(Long billId, Long participantId) {
        SplitBill bill = splitBillRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found with ID: " + billId));

        for (SplitBillParticipant p : bill.getParticipants()) {
            if (p.getId().equals(participantId)) {
                p.setSettled(true);
                p.setSettledDate(LocalDate.now());
                break;
            }
        }
        return splitBillRepository.save(bill);
    }

    @Transactional
    public void deleteBill(Long id) {
        splitBillRepository.deleteById(id);
    }

    public Map<String, Object> calculateNetBalance() {
        List<SplitBill> bills = splitBillRepository.findAll();
        BigDecimal owedToUser = BigDecimal.ZERO;
        BigDecimal userOwes = BigDecimal.ZERO;

        for (SplitBill bill : bills) {
            for (SplitBillParticipant p : bill.getParticipants()) {
                if (!p.isSettled()) {
                    if (p.getName().toLowerCase().contains("you") || p.getName().toLowerCase().contains("vrinda (you)")) {
                        userOwes = userOwes.add(p.getShareAmount());
                    } else {
                        owedToUser = owedToUser.add(p.getShareAmount());
                    }
                }
            }
        }

        BigDecimal net = owedToUser.subtract(userOwes);
        Map<String, Object> map = new HashMap<>();
        map.put("owedToYou", owedToUser);
        map.put("youOwe", userOwes);
        map.put("netBalance", net);
        map.put("summaryText", net.compareTo(BigDecimal.ZERO) >= 0 ? "You are owed more than you owe." : "You owe more than you are owed.");
        return map;
    }
}
