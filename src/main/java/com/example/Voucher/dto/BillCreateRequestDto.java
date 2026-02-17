package com.example.Voucher.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BillCreateRequestDto {
    @NotNull(message = "User id is required")
    @Positive(message = "User id must be greater than zero")
    private Long userId;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be greater than zero")
    private BigDecimal totalAmount;
}
