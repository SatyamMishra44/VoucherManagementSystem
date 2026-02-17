package com.example.Voucher.repository;


import com.example.Voucher.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher,Long> {

    //why i write optional here because the
    // - A voucher with the given code may or may not exit then optional will
    // Force the caller to handle the "not found" case explicitly that will Prevent NUllPointerException
    Optional<Voucher> findByCode(String code);

    Optional<Voucher> findByIdAndAssignedUserId(Long id, Long assignedUserId);

    Optional<Voucher> findByCodeAndAssignedUserId(String code, Long assignedUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select v from Voucher v
            where v.code = :code
              and v.assignedUser.id = :assignedUserId
            """)
    Optional<Voucher> findByCodeAndAssignedUserIdForUpdate(
            @Param("code") String code,
            @Param("assignedUserId") Long assignedUserId
    );

    List<Voucher> findByAssignedUserId(Long assignedUserId);

    @Query("""
            select v from Voucher v
            where v.assignedUser.id = :assignedUserId
              and v.isEnabled = true
              and v.isRedeemed = false
              and :today between v.startDate and v.expiryDate
            """)
    List<Voucher> findEligibleVouchersForUser(
            @Param("assignedUserId") Long assignedUserId,
            @Param("today") LocalDate today
    );
}
