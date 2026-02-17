package com.example.Voucher.dto;


import com.example.Voucher.entity.Voucher;
import lombok.Data;

import java.time.LocalDate;
import java.math.BigDecimal;

@Data
public class VoucherResponseDto {
    private Long id;

    private String code;

    private Double discountPercentage;

    private BigDecimal minBillAmount;

    private LocalDate startDate;

    private LocalDate expiryDate;

    private LocalDate createdAt;

    private Boolean active;

    private Long assignedUserId;

    private Boolean redeemed;

    private Long redeemedBillId;

    public static VoucherResponseDto fromEntity(Voucher voucher) {
        VoucherResponseDto dto = new VoucherResponseDto();
        dto.id = voucher.getId();
        dto.code = voucher.getCode();
        dto.discountPercentage = Double.valueOf(voucher.getDiscountPercentage());
        dto.minBillAmount = voucher.getMinBillAmount();
        dto.startDate = voucher.getStartDate();
        dto.expiryDate = voucher.getExpiryDate();
        dto.active = voucher.isEnabled();
        dto.assignedUserId = voucher.getAssignedUser().getId();
        dto.redeemed = voucher.isRedeemed();
        dto.redeemedBillId = voucher.getRedeemedBill() != null ? voucher.getRedeemedBill().getId() : null;
        dto.createdAt = LocalDate.from(voucher.getCreatedAt());
        return dto;
    }
}
