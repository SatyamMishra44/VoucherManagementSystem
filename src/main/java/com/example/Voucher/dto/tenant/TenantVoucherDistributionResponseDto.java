package com.example.Voucher.dto.tenant;

import com.example.Voucher.entity.TenantVoucherDistribution;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TenantVoucherDistributionResponseDto {

    private Long distributionId;
    private Long inventoryId;
    private String voucherCode;
    private Long distributedToUserId;
    private Long distributedByUserId;
    private Integer quantityDistributed;
    private BigDecimal totalDistributedAmount;
    private LocalDateTime createdAt;

    public static TenantVoucherDistributionResponseDto fromEntity(TenantVoucherDistribution distribution) {
        TenantVoucherDistributionResponseDto dto = new TenantVoucherDistributionResponseDto();
        dto.distributionId = distribution.getId();
        dto.inventoryId = distribution.getTenantVoucherInventory().getId();
        dto.voucherCode = distribution.getTenantVoucherInventory().getVoucherTemplate().getCode();
        dto.distributedToUserId = distribution.getDistributedToUser().getId();
        dto.distributedByUserId = distribution.getDistributedByUser().getId();
        dto.quantityDistributed = distribution.getQuantityDistributed();
        dto.totalDistributedAmount = distribution.getTotalDistributedAmount();
        dto.createdAt = distribution.getCreatedAt();
        return dto;
    }

    public Long getDistributionId() {
        return distributionId;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public Long getDistributedToUserId() {
        return distributedToUserId;
    }

    public Long getDistributedByUserId() {
        return distributedByUserId;
    }

    public Integer getQuantityDistributed() {
        return quantityDistributed;
    }

    public BigDecimal getTotalDistributedAmount() {
        return totalDistributedAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
