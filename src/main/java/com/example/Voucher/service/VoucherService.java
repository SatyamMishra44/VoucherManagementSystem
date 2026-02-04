package com.example.Voucher.service;

import com.example.Voucher.entity.Voucher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface VoucherService {

    //create a new voucher
    Voucher createVoucher(Voucher voucher);

    // Enable or disable the voucher status
    Voucher updateVoucherStatus(Long VoucherId,boolean isEnabled);


    //Get voucherById optional will force to think about nullPointerException
    Optional<Voucher> getVoucherById(Long VoucherId);

    Optional<Voucher> getVoucherByCode(String code);

    // Get all vouchers
    List<Voucher> getAllVouchers();

    // Validate voucher before redemption
    Voucher validateVoucher(String code);

    // Increment used count after successful redemption
    void incrementVoucherUsage(Long voucherId);
}
