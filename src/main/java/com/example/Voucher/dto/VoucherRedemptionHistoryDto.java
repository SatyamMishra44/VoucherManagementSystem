package com.example.Voucher.dto;

import com.example.Voucher.entity.VoucherRedemption;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoucherRedemptionHistoryDto {
    private Long redemptionId;
    private Long userId;
    private String voucherCode;
    private Long transactionId;
    private Integer totalAmount;
    private Integer finalAmount;
    private Integer discountApplied;
    private LocalDateTime redeemedAt;

    public static VoucherRedemptionHistoryDto fromEntity(VoucherRedemption redemption) {
        VoucherRedemptionHistoryDto dto = new VoucherRedemptionHistoryDto();
        dto.redemptionId = redemption.getId();
        dto.userId = redemption.getUser().getId();
        dto.voucherCode = redemption.getVoucher().getCode();
        dto.transactionId = redemption.getTransaction().getId();
        dto.totalAmount = redemption.getTransaction().getTotalAmount();
        dto.finalAmount = redemption.getTransaction().getFinalAmount();
        dto.discountApplied = redemption.getDiscountApplied();
        dto.redeemedAt = redemption.getRedeemedAt();
        return dto;
    }
}
