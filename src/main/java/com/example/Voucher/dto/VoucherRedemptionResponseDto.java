package com.example.Voucher.dto;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class VoucherRedemptionResponseDto {
    private String voucherCode;

    private BigDecimal originalBillAmount;

    private Double discountPercentage;

    private BigDecimal discountAmount;

    private BigDecimal finalPayableAmount;

    private Boolean voucherApplied;

    private String message;
}
