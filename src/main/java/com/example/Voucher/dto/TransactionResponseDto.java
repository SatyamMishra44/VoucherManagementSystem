package com.example.Voucher.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionResponseDto {

    private Long transactionId;

    private Long userId;

    private Long billId;

    private Double transactionAmount;

    private String paymentMethod;

    private String transactionStatus;
    // SUCCESS, FAILED, PENDING

    private LocalDateTime transactionTime;
}
