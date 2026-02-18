package com.example.Voucher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class VoucherTemplateCreateRequestDto {

    @NotBlank(message = "Voucher code is required")
    private String code;

    @NotNull(message = "Unit value is required")
    @Positive(message = "Unit value must be greater than zero")
    private BigDecimal unitValue;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;
}
