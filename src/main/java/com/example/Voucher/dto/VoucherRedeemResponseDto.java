package com.example.Voucher.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherRedeemResponseDto {

    private Long redemptionId;
    private Long userVoucherId;
    private Long billId;
    private BigDecimal billAmount;
    private BigDecimal redeemedAmount;
    private BigDecimal payableAmount;
    private BigDecimal remainingBalance;
    private String userVoucherStatus;
    private LocalDateTime redeemedAt;
    private String message;
}
