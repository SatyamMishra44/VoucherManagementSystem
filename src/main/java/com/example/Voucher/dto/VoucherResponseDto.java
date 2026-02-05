package com.example.Voucher.dto;


import com.example.Voucher.entity.Voucher;
import lombok.Data;

import java.time.LocalDate;

@Data
public class VoucherResponseDto {
    private Long id;

    private String code;

    private Double discountPercentage;

    private Double minBillAmount;

    private Double maxDiscountAmount;

    private LocalDate createdAt;

    private Integer usageLimit;

    private Integer maxGlobalUses;

    private Integer usedCount;

    private Boolean active;

    public static VoucherResponseDto fromEntity(Voucher voucher) {
        VoucherResponseDto dto = new VoucherResponseDto();
        dto.id = voucher.getId();
        dto.code = voucher.getCode();
        dto.discountPercentage = Double.valueOf(voucher.getDiscountPercentage());
        dto.usageLimit = voucher.getMaxGlobalUses();
        dto.maxGlobalUses = voucher.getMaxGlobalUses();
        dto.usedCount = voucher.getUsedCount();
        dto.active = voucher.isEnabled();
        dto.createdAt = LocalDate.from(voucher.getCreatedAt());
        return dto;
    }
}
