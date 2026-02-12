package com.example.Voucher.repository;


import com.example.Voucher.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher,Long> {

    //why i write optional here because the
    // - A voucher with the given code may or may not exit then optional will
    // Force the caller to handle the "not found" case explicitly that will Prevent NUllPointerException
    Optional<Voucher> findByCode(String code);

    @Modifying
    @Query("""
            update Voucher v
            set v.usedCount = v.usedCount + 1
            where v.id = :voucherId
              and v.isEnabled = true
              and v.usedCount < v.maxGlobalUses
            """)
    int incrementUsageIfAvailable(@Param("voucherId") Long voucherId);
}
