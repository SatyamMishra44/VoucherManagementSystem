package com.example.Voucher.dto;

import com.example.Voucher.entity.RedemptionHistory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RedemptionHistoryResponseDto {

    private Long redemptionId;
    private Long userVoucherId;
    private String voucherCode;
    private Long billId;
    private BigDecimal redeemedAmount;
    private BigDecimal remainingBalanceAfter;
    private LocalDateTime redeemedAt;

    public static RedemptionHistoryResponseDto fromEntity(RedemptionHistory history) {
        RedemptionHistoryResponseDto dto = new RedemptionHistoryResponseDto();
        dto.setRedemptionId(history.getId());
        dto.setUserVoucherId(history.getUserVoucher().getId());
        dto.setVoucherCode(history.getUserVoucher().getVoucherTemplate().getCode());
        dto.setBillId(history.getBill() != null ? history.getBill().getId() : null);
        dto.setRedeemedAmount(history.getRedeemedAmount());
        dto.setRemainingBalanceAfter(history.getRemainingBalanceAfter());
        dto.setRedeemedAt(history.getRedeemedAt());
        return dto;
    }
}
