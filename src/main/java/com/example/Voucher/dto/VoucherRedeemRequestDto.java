package com.example.Voucher.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VoucherRedeemRequestDto {

    @NotNull(message = "User voucher id is required")
    @Positive(message = "User voucher id must be greater than zero")
    private Long userVoucherId;

    // Required: bill amount is always fetched from DB by bill id.
    @NotNull(message = "Bill id is required")
    @Positive(message = "Bill id must be greater than zero")
    private Long billId;
}
