package com.spendly.repository;

import com.spendly.entity.ParentAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParentAlertRepository extends JpaRepository<ParentAlert, Long> {
    List<ParentAlert> findAllByOrderByTimestampDesc();
    List<ParentAlert> findByIsReadFalseOrderByTimestampDesc();
}
