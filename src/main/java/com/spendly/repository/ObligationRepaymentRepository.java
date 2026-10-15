package com.spendly.repository;

import com.spendly.entity.ObligationRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObligationRepaymentRepository extends JpaRepository<ObligationRepayment, Long> {
    List<ObligationRepayment> findByObligationId(Long obligationId);
}
