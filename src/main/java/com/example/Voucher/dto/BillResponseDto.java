package com.example.Voucher.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BillResponseDto {

    private Long billId;

    private Long userId;

    private Double originalAmount;

    private Double discountAmount;

    private Double finalPayableAmount;



   private String billStatus;//status is usefull for UI ,audit and logs
    // CREATED, DISCOUNT_APPLIED, PAID

    private LocalDateTime createdAt;
}

