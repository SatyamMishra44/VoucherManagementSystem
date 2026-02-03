package com.example.Voucher.repository;


import com.example.Voucher.entity.VoucherRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption,Long> {
    boolean existsByVoucherIdAndUserId(Long voucherId,Long userId); // prevent same user from redeeming same voucher again and again
    long countByVoucherid(Long voucherId);// track the voucher usage count

}
