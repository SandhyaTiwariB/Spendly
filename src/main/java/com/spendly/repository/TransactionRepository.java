package com.spendly.repository;

import com.spendly.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findAllByOrderByTransactionDateDescIdDesc();

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(LocalDate startDate, LocalDate endDate);

    List<Transaction> findByCategoryIgnoreCaseOrderByTransactionDateDescIdDesc(String category);

    Optional<Transaction> findByFingerprint(String fingerprint);

    boolean existsByFingerprint(String fingerprint);

    boolean existsByCategoryIgnoreCase(String category);

    @Query("SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate ASC")
    List<Transaction> findForPeriodChronological(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE (:category IS NULL OR LOWER(t.category) = LOWER(:category)) AND " +
           "(:query IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.category) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> searchTransactions(@Param("category") String category, @Param("query") String query);

    long deleteByIsDemoDataTrue();

    long countByIsDemoDataTrue();

    long countByIsDemoDataFalse();
}
