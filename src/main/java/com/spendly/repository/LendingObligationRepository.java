package com.spendly.repository;

import com.spendly.entity.LendingObligation;
import com.spendly.entity.ObligationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LendingObligationRepository extends JpaRepository<LendingObligation, Long> {
    List<LendingObligation> findAllByOrderByStartDateDesc();
    List<LendingObligation> findByType(ObligationType type);
}
