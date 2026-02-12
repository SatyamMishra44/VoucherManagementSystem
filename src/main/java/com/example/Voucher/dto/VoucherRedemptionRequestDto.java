package com.example.Voucher.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VoucherRedemptionRequestDto {
    @NotBlank(message = "Voucher code is required")
    private String voucherCode;

    @NotNull(message = "Bill amount is required")
    @Positive(message = "Bill amount must be greater than zero")
    private Double billAmount;
}
