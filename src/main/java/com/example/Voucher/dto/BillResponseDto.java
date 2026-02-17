package com.example.Voucher.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BillResponseDto {

    private Long billId;

    private Long userId;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;
}
