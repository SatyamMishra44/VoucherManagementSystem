package com.example.Voucher.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransactionCreateRequestDto {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Bill id is required")
    private Long billId;

    @NotNull(message = "Transaction amount is required")
    @Positive(message = "Transaction amount must be greater than zero")
    private Double transactionAmount;

    private String paymentMethod;
    // Example: UPI, CARD, NET_BANKING
}
