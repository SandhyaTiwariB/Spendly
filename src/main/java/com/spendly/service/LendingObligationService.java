package com.spendly.service;

import com.spendly.dto.LendingRequest;
import com.spendly.dto.RepaymentRequest;
import com.spendly.entity.LendingObligation;
import com.spendly.entity.ObligationRepayment;
import com.spendly.entity.ObligationStatus;
import com.spendly.entity.ObligationType;
import com.spendly.repository.LendingObligationRepository;
import com.spendly.repository.ObligationRepaymentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class LendingObligationService {

    private final LendingObligationRepository obligationRepository;
    private final ObligationRepaymentRepository repaymentRepository;

    public LendingObligationService(LendingObligationRepository obligationRepository,
                                    ObligationRepaymentRepository repaymentRepository) {
        this.obligationRepository = obligationRepository;
        this.repaymentRepository = repaymentRepository;
    }

    @PostConstruct
    public void initDefaultObligations() {
        if (obligationRepository.count() == 0) {
            // Rahul: Lent 2000
            LendingObligation o1 = new LendingObligation("Rahul", ObligationType.LENT, new BigDecimal("2000.00"),
                    LocalDate.now().minusDays(15), LocalDate.now().plusDays(10), "For September rent");
            o1.setStatus(ObligationStatus.PENDING);
            obligationRepository.save(o1);

            // Aadya: Lent 1250
            LendingObligation o2 = new LendingObligation("Aadya", ObligationType.LENT, new BigDecimal("1250.00"),
                    LocalDate.now().minusDays(10), LocalDate.now().plusDays(5), "Emergency cash");
            o2.setStatus(ObligationStatus.PENDING);
            obligationRepository.save(o2);

            // Rohit: Lent 5250, partially repaid 3650, remaining 1600
            LendingObligation o3 = new LendingObligation("Rohit", ObligationType.LENT, new BigDecimal("5250.00"),
                    LocalDate.now().minusDays(30), LocalDate.now().minusDays(5), "College project materials");
            o3.addRepayment(new ObligationRepayment(new BigDecimal("3650.00"), LocalDate.now().minusDays(12), "UPI transfer"));
            o3.setStatus(ObligationStatus.PENDING);
            obligationRepository.save(o3);

            // Riya: Borrowed 720
            LendingObligation o4 = new LendingObligation("Riya", ObligationType.BORROWED, new BigDecimal("720.00"),
                    LocalDate.now().minusDays(7), LocalDate.now().plusDays(2), "Dinner + cab");
            o4.setStatus(ObligationStatus.DUE_SOON);
            obligationRepository.save(o4);

            // Om: Borrowed 600
            LendingObligation o5 = new LendingObligation("Om", ObligationType.BORROWED, new BigDecimal("600.00"),
                    LocalDate.now().minusDays(4), LocalDate.now().plusDays(15), "Movie tickets");
            o5.setStatus(ObligationStatus.PENDING);
            obligationRepository.save(o5);

            // Ananya: Borrowed 1880, repaid 1880 (Settled)
            LendingObligation o6 = new LendingObligation("Ananya", ObligationType.BORROWED, new BigDecimal("1880.00"),
                    LocalDate.now().minusDays(40), LocalDate.now().minusDays(20), "Concert pass");
            o6.addRepayment(new ObligationRepayment(new BigDecimal("1880.00"), LocalDate.now().minusDays(22), "Settled in full via GPay"));
            o6.setStatus(ObligationStatus.SETTLED);
            obligationRepository.save(o6);
        }
    }

    public List<LendingObligation> getAllObligations() {
        return obligationRepository.findAllByOrderByStartDateDesc();
    }

    public Optional<LendingObligation> getById(Long id) {
        return obligationRepository.findById(id);
    }

    @Transactional
    public LendingObligation createObligation(LendingRequest request) {
        LendingObligation obligation = new LendingObligation(
                request.getPersonName().trim(),
                request.getType(),
                request.getAmount(),
                request.getStartDate() != null ? request.getStartDate() : LocalDate.now(),
                request.getDueDate(),
                request.getNotes()
        );
        return obligationRepository.save(obligation);
    }

    @Transactional
    public LendingObligation recordRepayment(Long id, RepaymentRequest request) {
        LendingObligation obligation = obligationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Obligation not found with ID: " + id));

        ObligationRepayment repayment = new ObligationRepayment(
                request.getAmount(),
                request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now(),
                request.getNotes()
        );

        obligation.addRepayment(repayment);
        return obligationRepository.save(obligation);
    }

    @Transactional
    public LendingObligation markSettled(Long id) {
        LendingObligation obligation = obligationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Obligation not found with ID: " + id));

        if (obligation.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            ObligationRepayment finalPayment = new ObligationRepayment(
                    obligation.getBalance(),
                    LocalDate.now(),
                    "Marked as settled in full"
            );
            obligation.addRepayment(finalPayment);
        }
        obligation.setStatus(ObligationStatus.SETTLED);
        obligation.setBalance(BigDecimal.ZERO);
        return obligationRepository.save(obligation);
    }

    @Transactional
    public void deleteObligation(Long id) {
        obligationRepository.deleteById(id);
    }

    public Map<String, Object> getSummary() {
        List<LendingObligation> all = obligationRepository.findAll();

        BigDecimal totalLent = BigDecimal.ZERO;
        BigDecimal lentOutstanding = BigDecimal.ZERO;
        BigDecimal totalBorrowed = BigDecimal.ZERO;
        BigDecimal borrowedOutstanding = BigDecimal.ZERO;

        List<Map<String, Object>> owedToYouList = new ArrayList<>();
        List<Map<String, Object>> youOweList = new ArrayList<>();

        for (LendingObligation o : all) {
            if (o.getType() == ObligationType.LENT) {
                totalLent = totalLent.add(o.getAmount());
                lentOutstanding = lentOutstanding.add(o.getBalance());
                if (o.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", o.getPersonName());
                    item.put("amount", o.getBalance());
                    owedToYouList.add(item);
                }
            } else {
                totalBorrowed = totalBorrowed.add(o.getAmount());
                borrowedOutstanding = borrowedOutstanding.add(o.getBalance());
                if (o.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", o.getPersonName());
                    item.put("amount", o.getBalance());
                    youOweList.add(item);
                }
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalLent", totalLent);
        summary.put("lentOutstanding", lentOutstanding);
        summary.put("totalBorrowed", totalBorrowed);
        summary.put("borrowedOutstanding", borrowedOutstanding);
        summary.put("owedToYouList", owedToYouList);
        summary.put("youOweList", youOweList);
        return summary;
    }
}
