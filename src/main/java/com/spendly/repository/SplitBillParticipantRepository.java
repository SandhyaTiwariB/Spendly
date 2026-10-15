package com.spendly.repository;

import com.spendly.entity.SplitBillParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SplitBillParticipantRepository extends JpaRepository<SplitBillParticipant, Long> {
    List<SplitBillParticipant> findBySplitBillId(Long splitBillId);
}
