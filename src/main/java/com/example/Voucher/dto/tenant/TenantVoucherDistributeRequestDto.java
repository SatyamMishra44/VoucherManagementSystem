package com.example.Voucher.dto.tenant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class TenantVoucherDistributeRequestDto {

    @NotNull(message = "Inventory id is required")
    @Positive(message = "Inventory id must be greater than zero")
    private Long inventoryId;

    @NotNull(message = "User id is required")
    @Positive(message = "User id must be greater than zero")
    private Long userId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
