package com.example.Voucher.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherRedeemRequestDto {

    @NotNull(message = "User voucher id is required")
    @Positive(message = "User voucher id must be greater than zero")
    private Long userVoucherId;

    @NotNull(message = "Bill amount is required")
    @Positive(message = "Bill amount must be greater than zero")
    private BigDecimal billAmount;

    // Optional: if provided, redemption will be tied to this bill for audit and transaction records.
    @Positive(message = "Bill id must be greater than zero")
    private Long billId;
}
