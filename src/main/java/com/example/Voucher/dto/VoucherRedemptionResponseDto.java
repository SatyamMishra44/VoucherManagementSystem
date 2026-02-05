package com.example.Voucher.dto;


import lombok.Data;

@Data
public class VoucherRedemptionResponseDto {
    private String voucherCode;

    private Double originalBillAmount;

    private Double discountPercentage;

    private Double discountAmount;

    private Double finalPayableAmount;

    private Boolean voucherApplied;

    private String message;
}
