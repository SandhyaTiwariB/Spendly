package com.spendly.repository;

import com.spendly.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findAllByOrderByYearMonthDescCategoryAsc();

    List<Budget> findByYearMonth(String yearMonth);

    Optional<Budget> findByCategoryIgnoreCaseAndYearMonth(String category, String yearMonth);

    boolean existsByCategoryIgnoreCaseAndYearMonth(String category, String yearMonth);
}
