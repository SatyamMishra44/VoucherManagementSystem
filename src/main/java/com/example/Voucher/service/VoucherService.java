package com.example.Voucher.service;

import com.example.Voucher.entity.Voucher;

import java.util.List;
import java.util.Optional;

public interface VoucherService {

    //create a new voucher
    Voucher createVoucher(Voucher voucher);

    // Enable or disable the voucher status
    Voucher updateVoucherStatus(Long VoucherId,boolean isEnabled);


    //Get voucherById optional will force to think about nullPointerException
    Optional<Voucher> getVoucherById(Long VoucherId);

    Optional<Voucher> getVoucherByCode(String code);

    Optional<Voucher> getVoucherByIdForUser(Long voucherId, Long userId);

    Optional<Voucher> getVoucherByCodeForUser(String code, Long userId);

    // Get all vouchers
    List<Voucher> getAllVouchers();

    List<Voucher> getAllVouchersForUser(Long userId);

    // Get eligible vouchers for a user (based on voucher rules and user usage)
    List<Voucher> getEligibleVouchers(Long userId);

    // Validate voucher before redemption
    Voucher validateVoucher(String code);

}
