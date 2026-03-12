package com.example.Voucher.repository;

import com.example.Voucher.entity.UserVoucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    List<UserVoucher> findByUserIdAndTenantIdOrderByPurchasedAtDesc(Long userId, Long tenantId);
    @EntityGraph(attributePaths = {"voucherTemplate", "user"})
    List<UserVoucher> findByTenantIdAndPurchasedAtBetweenOrderByPurchasedAtAsc(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to
    );
    @EntityGraph(attributePaths = {"voucherTemplate", "user"})
    List<UserVoucher> findByUserIdAndTenantIdAndPurchasedAtBetweenOrderByPurchasedAtAsc(
            Long userId,
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select uv from UserVoucher uv
            where uv.id = :id
              and uv.user.id = :userId
              and uv.tenantId = :tenantId
            """)
    Optional<UserVoucher> findByIdAndUserIdForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("tenantId") Long tenantId
    );

    @Query("""
            SELECT uv
            FROM UserVoucher uv
            WHERE uv.tenantId = :tenantId
              AND (:assignedUserId IS NULL OR uv.user.id = :assignedUserId)
              AND (:minVoucherAmount IS NULL OR uv.totalPurchasedAmount >= :minVoucherAmount)
              AND (:maxVoucherAmount IS NULL OR uv.totalPurchasedAmount <= :maxVoucherAmount)
              AND (:status IS NULL OR uv.status = :status)
              AND (:issuedFrom IS NULL OR uv.purchasedAt >= :issuedFrom)
              AND (:issuedTo IS NULL OR uv.purchasedAt <= :issuedTo)
              AND (:expiryFrom IS NULL OR uv.voucherTemplate.expiryDate >= :expiryFrom)
              AND (:expiryTo IS NULL OR uv.voucherTemplate.expiryDate <= :expiryTo)
              AND (
                    :redemptionState IS NULL
                    OR (:redemptionState = 'NOT_REDEEMED'
                        AND uv.remainingBalance = uv.totalPurchasedAmount)
                    OR (:redemptionState = 'PARTIALLY_REDEEMED'
                        AND uv.remainingBalance > 0
                        AND uv.remainingBalance < uv.totalPurchasedAmount)
                    OR (:redemptionState = 'FULLY_REDEEMED'
                        AND uv.remainingBalance = 0)
                  )
            ORDER BY uv.purchasedAt DESC
            """)
    List<UserVoucher> findAllByTenantIdWithAdminFilters(
            @Param("tenantId") Long tenantId,
            @Param("assignedUserId") Long assignedUserId,
            @Param("minVoucherAmount") BigDecimal minVoucherAmount,
            @Param("maxVoucherAmount") BigDecimal maxVoucherAmount,
            @Param("redemptionState") String redemptionState,
            @Param("issuedFrom") LocalDateTime issuedFrom,
            @Param("issuedTo") LocalDateTime issuedTo,
            @Param("expiryFrom") LocalDate expiryFrom,
            @Param("expiryTo") LocalDate expiryTo,
            @Param("status") com.example.Voucher.entity.UserVoucherStatus status
    );
}
