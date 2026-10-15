package com.spendly.repository;

import com.spendly.entity.ParentActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ParentActivityLogRepository extends JpaRepository<ParentActivityLog, Long> {

    List<ParentActivityLog> findAllByOrderByTimestampDesc();

    @Query("SELECT l FROM ParentActivityLog l WHERE " +
           "(:childAccount IS NULL OR LOWER(l.childAccount) LIKE LOWER(CONCAT('%', :childAccount, '%'))) AND " +
           "(:activityType IS NULL OR l.activityType = :activityType) AND " +
           "(:category IS NULL OR LOWER(l.category) = LOWER(:category)) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:startDate IS NULL OR l.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR l.timestamp <= :endDate) AND " +
           "(:search IS NULL OR LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.actor) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.reason) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY l.timestamp DESC")
    Page<ParentActivityLog> findFilteredLogs(
            @Param("childAccount") String childAccount,
            @Param("activityType") String activityType,
            @Param("category") String category,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            Pageable pageable);
}
