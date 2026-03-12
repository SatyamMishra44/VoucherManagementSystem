package com.example.Voucher.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponseDto {

    private Long transactionId;

    private Long userId;

    private Long billId;

    private BigDecimal transactionAmount;

    private BigDecimal finalAmount;

    private String paymentMethod;

    private String transactionStatus;
    // SUCCESS, FAILED, PENDING

    private LocalDateTime transactionTime;
}
