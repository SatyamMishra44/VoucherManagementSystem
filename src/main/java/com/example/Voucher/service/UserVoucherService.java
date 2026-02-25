package com.example.Voucher.service;

import com.example.Voucher.entity.RedemptionHistory;
import com.example.Voucher.entity.UserVoucher;

import java.util.List;

public interface UserVoucherService {

    UserVoucher purchaseVoucher(Long userId, String voucherCode, Integer quantity);

    RedemptionResult redeemVoucher(Long userId, Long userVoucherId, Long billId);

    List<UserVoucher> getUserVouchers(Long userId);

    List<RedemptionHistory> getUserRedemptionHistory(Long userId);
}
