package com.example.Voucher.repository;

import com.example.Voucher.entity.Transaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository //enable spring bean creation and talk to the database
public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    List<Transaction> findByUserIdAndTenantId(Long userId, Long tenantId);
    @EntityGraph(attributePaths = {"user", "bill"})
    List<Transaction> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long tenantId,
            LocalDateTime fromTime,
            LocalDateTime toTime
    );
    @EntityGraph(attributePaths = {"user", "bill"})
    List<Transaction> findByUserIdAndTenantIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long userId,
            Long tenantId,
            LocalDateTime fromTime,
            LocalDateTime toTime
    );

    @Query("""
            SELECT t
            FROM Transaction t
            WHERE t.tenantId = :tenantId
              AND (:userId IS NULL OR t.user.id = :userId)
              AND (:minTotalAmount IS NULL OR t.totalAmount >= :minTotalAmount)
              AND (:maxTotalAmount IS NULL OR t.totalAmount <= :maxTotalAmount)
              AND (:minFinalAmount IS NULL OR t.finalAmount >= :minFinalAmount)
              AND (:maxFinalAmount IS NULL OR t.finalAmount <= :maxFinalAmount)
              AND (:fromTime IS NULL OR t.createdAt >= :fromTime)
              AND (:toTime IS NULL OR t.createdAt <= :toTime)
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findAllByTenantIdWithFilters(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("minTotalAmount") BigDecimal minTotalAmount,
            @Param("maxTotalAmount") BigDecimal maxTotalAmount,
            @Param("minFinalAmount") BigDecimal minFinalAmount,
            @Param("maxFinalAmount") BigDecimal maxFinalAmount,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );
}
