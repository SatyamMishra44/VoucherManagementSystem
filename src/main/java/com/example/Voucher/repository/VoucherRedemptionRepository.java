package com.example.Voucher.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Voucher.entity.VoucherRedemption;

import java.util.List;

@Repository
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption,Long> {
    boolean existsByVoucherIdAndUserId(Long voucherId,Long userId); // prevent same user from redeeming same voucher again and again
    long countByVoucherId(Long voucherId);// track the voucher usage count
    List<VoucherRedemption> findByUserIdOrderByRedeemedAtDesc(Long userId);
    List<VoucherRedemption> findByVoucherIdOrderByRedeemedAtDesc(Long voucherId);
}
