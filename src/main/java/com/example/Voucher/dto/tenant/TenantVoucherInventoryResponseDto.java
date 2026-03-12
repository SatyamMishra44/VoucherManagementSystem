package com.example.Voucher.dto.tenant;

import com.example.Voucher.entity.TenantVoucherInventory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TenantVoucherInventoryResponseDto {

    private Long inventoryId;
    private Long tenantId;
    private Long voucherTemplateId;
    private String voucherCode;
    private BigDecimal unitValue;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private Integer quantityPurchasedTotal;
    private Integer quantityAvailable;
    private LocalDateTime updatedAt;

    public static TenantVoucherInventoryResponseDto fromEntity(TenantVoucherInventory inventory) {
        TenantVoucherInventoryResponseDto dto = new TenantVoucherInventoryResponseDto();
        dto.inventoryId = inventory.getId();
        dto.tenantId = inventory.getTenantId();
        dto.voucherTemplateId = inventory.getVoucherTemplate().getId();
        dto.voucherCode = inventory.getVoucherTemplate().getCode();
        dto.unitValue = inventory.getVoucherTemplate().getUnitValue();
        dto.startDate = inventory.getVoucherTemplate().getStartDate();
        dto.expiryDate = inventory.getVoucherTemplate().getExpiryDate();
        dto.quantityPurchasedTotal = inventory.getQuantityPurchasedTotal();
        dto.quantityAvailable = inventory.getQuantityAvailable();
        dto.updatedAt = inventory.getUpdatedAt();
        return dto;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getVoucherTemplateId() {
        return voucherTemplateId;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public BigDecimal getUnitValue() {
        return unitValue;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public Integer getQuantityPurchasedTotal() {
        return quantityPurchasedTotal;
    }

    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
