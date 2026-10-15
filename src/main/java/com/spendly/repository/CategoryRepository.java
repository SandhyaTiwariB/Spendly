package com.spendly.repository;

import com.spendly.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT c FROM Category c WHERE c.userId IS NULL OR (:userId IS NOT NULL AND c.userId = :userId) ORDER BY c.isCustom ASC, c.name ASC")
    List<Category> findVisibleCategories(@Param("userId") Long userId);

    List<Category> findByUserIdIsNullOrderByNameAsc();

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name)) AND (c.userId IS NULL OR (:userId IS NOT NULL AND c.userId = :userId))")
    boolean existsByNameIgnoreCaseForUser(@Param("name") String name, @Param("userId") Long userId);
}
