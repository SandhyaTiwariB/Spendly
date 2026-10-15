package com.spendly.repository;

import com.spendly.entity.ParentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentRuleRepository extends JpaRepository<ParentRule, Long> {
    List<ParentRule> findByChildAccount(String childAccount);
    Optional<ParentRule> findByChildAccountAndCategoryIgnoreCase(String childAccount, String category);
}
