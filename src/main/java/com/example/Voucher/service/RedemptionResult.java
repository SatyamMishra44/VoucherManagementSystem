package com.example.Voucher.service;

import com.example.Voucher.entity.UserVoucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RedemptionResult {

    private final Long redemptionId;
    private final Long billId;
    private final UserVoucher userVoucher;
    private final BigDecimal billAmount;
    private final BigDecimal redeemedAmount;
    private final BigDecimal payableAmount;
    private final LocalDateTime redeemedAt;

    public RedemptionResult(Long redemptionId,
                            Long billId,
                            UserVoucher userVoucher,
                            BigDecimal billAmount,
                            BigDecimal redeemedAmount,
                            BigDecimal payableAmount,
                            LocalDateTime redeemedAt) {
        this.redemptionId = redemptionId;
        this.billId = billId;
        this.userVoucher = userVoucher;
        this.billAmount = billAmount;
        this.redeemedAmount = redeemedAmount;
        this.payableAmount = payableAmount;
        this.redeemedAt = redeemedAt;
    }

    public Long getRedemptionId() {
        return redemptionId;
    }

    public Long getBillId() {
        return billId;
    }

    public UserVoucher getUserVoucher() {
        return userVoucher;
    }

    public BigDecimal getBillAmount() {
        return billAmount;
    }

    public BigDecimal getRedeemedAmount() {
        return redeemedAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }
}
