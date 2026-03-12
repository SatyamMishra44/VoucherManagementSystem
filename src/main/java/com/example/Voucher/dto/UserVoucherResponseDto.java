package com.example.Voucher.dto;

import com.example.Voucher.entity.UserVoucher;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserVoucherResponseDto {

    private Long id;
    private Long assignedUserId;
    private Long voucherTemplateId;
    private String voucherCode;
    private BigDecimal unitValue;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private Integer quantityPurchased;
    private BigDecimal totalPurchasedAmount;
    private BigDecimal remainingBalance;
    private String status;
    private LocalDateTime purchasedAt;

    public static UserVoucherResponseDto fromEntity(UserVoucher userVoucher) {
        UserVoucherResponseDto dto = new UserVoucherResponseDto();
        dto.setId(userVoucher.getId());
        dto.setAssignedUserId(userVoucher.getUser().getId());
        dto.setVoucherTemplateId(userVoucher.getVoucherTemplate().getId());
        dto.setVoucherCode(userVoucher.getVoucherTemplate().getCode());
        dto.setUnitValue(userVoucher.getVoucherTemplate().getUnitValue());
        dto.setStartDate(userVoucher.getVoucherTemplate().getStartDate());
        dto.setExpiryDate(userVoucher.getVoucherTemplate().getExpiryDate());
        dto.setQuantityPurchased(userVoucher.getQuantityPurchased());
        dto.setTotalPurchasedAmount(userVoucher.getTotalPurchasedAmount());
        dto.setRemainingBalance(userVoucher.getRemainingBalance());
        dto.setStatus(userVoucher.getStatus().name());
        dto.setPurchasedAt(userVoucher.getPurchasedAt());
        return dto;
    }
}
