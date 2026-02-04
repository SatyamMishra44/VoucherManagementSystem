package com.example.Voucher.service;

import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.VoucherRedemption;

public interface VoucherRedemptionService {

    // this method represents the entire voucher redemption flow
    Transaction redeemVoucher(Long userId,
                              String voucherCode,
                              Double billAmount
    );


    //fetch redemption history by its Id
    //used for audit,debugging...etc
    VoucherRedemption getRedemptionById(Long redemptionId);
}
