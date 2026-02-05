package com.example.Voucher.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BillCreateRequestDto {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Original bill amount is required")
    @Positive(message = "Bill amount must be greater than zero")
    private Double originalAmount;
}

